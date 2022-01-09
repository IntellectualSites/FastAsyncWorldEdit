package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.fastasyncworldedit.core.extent.filter.block.CharFilterBlock;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.EmptyBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.ExtentBatchProcessorHolder;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharSetBlocks;
import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkHolder;
import com.fastasyncworldedit.core.queue.implementation.chunk.NullChunk;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.wrappers.WorldWrapper;
import com.google.common.util.concurrent.Futures;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single threaded implementation for IQueueExtent (still abstract) - Does not implement creation of
 * chunks (that has to implemented by the platform e.g., Bukkit)
 * <p>
 * This queue is reusable {@link #init(Extent, IChunkCache, IChunkCache)}
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SingleThreadQueueExtent extends ExtentBatchProcessorHolder implements IQueueExtent<IQueueChunk> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    // Pool discarded chunks for reuse (can safely be cleared by another thread)
    // private static final ConcurrentLinkedQueue<IChunk> CHUNK_POOL = new ConcurrentLinkedQueue<>();
    // Chunks currently being queued / worked on
    private final Long2ObjectLinkedOpenHashMap<IQueueChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();
    private final ConcurrentLinkedQueue<Future> submissions = new ConcurrentLinkedQueue<>();
    private final ReentrantLock getChunkLock = new ReentrantLock();
    private World world = null;
    private int minY = 0;
    private int maxY = 255;
    private IChunkCache<IChunkGet> cacheGet;
    private IChunkCache<IChunkSet> cacheSet;
    private boolean initialized;
    private Thread currentThread;
    // Last access pointers
    private IQueueChunk lastChunk;
    private long lastPair = Long.MAX_VALUE;
    private boolean enabledQueue = true;
    private boolean fastmode = false;
    // Array for lazy avoidance of concurrent modification exceptions and needless overcomplication of code (synchronisation is
    // not very important)
    private boolean[] faweExceptionReasonsUsed = new boolean[FaweException.Type.values().length];
    private int lastException = Integer.MIN_VALUE;
    private int exceptionCount = 0;

    public SingleThreadQueueExtent() {
    }

    /**
     * New instance given inclusive world height bounds.
     */
    public SingleThreadQueueExtent(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * Safety check to ensure that the thread being used matches the one being initialized on. - Can
     * be removed later
     */
    private void checkThread() {
        if (Thread.currentThread() != currentThread && currentThread != null) {
            throw new UnsupportedOperationException(
                    "This class must be used from a single thread. Use multiple queues for concurrent operations");
        }
    }

    @Override
    public void enableQueue() {
        enabledQueue = true;
    }

    @Override
    public void disableQueue() {
        enabledQueue = false;
    }

    @Override
    public IChunkGet getCachedGet(int chunkX, int chunkZ) {
        return cacheGet.get(chunkX, chunkZ);
    }

    @Override
    public IChunkSet getCachedSet(int chunkX, int chunkZ) {
        return cacheSet.get(chunkX, chunkZ);
    }

    @Override
    public boolean isFastMode() {
        return fastmode;
    }

    @Override
    public void setFastMode(boolean fastmode) {
        this.fastmode = fastmode;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    /**
     * Sets the cached boolean array of length {@code FaweException.Type.values().length} that determines if a thrown
     * {@link FaweException} of type {@link FaweException.Type} should be output to console, rethrown to attempt to be visible
     * to the player, etc. Allows the same array to be used as widely as possible across the edit to avoid spam to console.
     *
     * @param faweExceptionReasonsUsed boolean array that should be cached where this method is called from of length {@code
     *                                 FaweException.Type.values().length}
     */
    public void setFaweExceptionArray(final boolean[] faweExceptionReasonsUsed) {
        this.faweExceptionReasonsUsed = faweExceptionReasonsUsed;
    }

    /**
     * Resets the queue.
     */
    protected synchronized void reset() {
        if (!this.initialized) {
            return;
        }
        if (!this.chunks.isEmpty()) {
            for (IChunk chunk : this.chunks.values()) {
                chunk.recycle();
            }
            getChunkLock.lock();
            this.chunks.clear();
            getChunkLock.unlock();
        }
        this.enabledQueue = true;
        this.lastChunk = null;
        this.lastPair = Long.MAX_VALUE;
        this.currentThread = null;
        this.initialized = false;
        this.setProcessor(EmptyBatchProcessor.getInstance());
        this.setPostProcessor(EmptyBatchProcessor.getInstance());
        this.world = null;
    }

    /**
     * Initialize the queue
     */
    @Override
    public synchronized void init(Extent extent, IChunkCache<IChunkGet> get, IChunkCache<IChunkSet> set) {
        reset();
        this.minY = extent.getMinY();
        this.maxY = extent.getMaxY();
        currentThread = Thread.currentThread();
        if (get == null) {
            get = (x, z) -> {
                throw new UnsupportedOperationException();
            };
        }
        if (set == null) {
            set = (x, z) -> CharSetBlocks.newInstance();
        }
        this.cacheGet = get;
        this.cacheSet = set;
        this.setProcessor(EmptyBatchProcessor.getInstance());
        this.setPostProcessor(EmptyBatchProcessor.getInstance());
        initialized = true;

        if (extent.isWorld()) {
            world = (World) ((extent instanceof PassthroughExtent) ? ((PassthroughExtent) extent).getExtent() : extent);
        } else if (extent instanceof EditSession) {
            world = ((EditSession) extent).getWorld();
        } else {
            world = WorldWrapper.unwrap(extent);
        }
    }

    @Override
    public int size() {
        return chunks.size() + submissions.size();
    }

    @Override
    public boolean isEmpty() {
        return chunks.isEmpty() && submissions.isEmpty();
    }

    @Override
    public <V extends Future<V>> V submit(IQueueChunk chunk) {
        if (lastChunk == chunk) {
            lastPair = Long.MAX_VALUE;
            lastChunk = null;
        }
        final long index = MathMan.pairInt(chunk.getX(), chunk.getZ());
        getChunkLock.lock();
        chunks.remove(index, chunk);
        getChunkLock.unlock();
        V future = submitUnchecked(chunk);
        submissions.add(future);
        return future;
    }

    /**
     * Submit without first checking that it has been removed from the chunk map
     */
    private <V extends Future<V>> V submitUnchecked(IQueueChunk chunk) {
        if (chunk.isEmpty()) {
            chunk.recycle();
            Future result = Futures.immediateFuture(null);
            return (V) result;
        }

        if (Fawe.isMainThread()) {
            V result = (V) chunk.call();
            if (result == null) {
                return (V) (Future) Futures.immediateFuture(null);
            } else {
                return result;
            }
        }

        return (V) Fawe.instance().getQueueHandler().submit(chunk);
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        cacheGet.trim(aggressive);
        cacheSet.trim(aggressive);
        if (Thread.currentThread() == currentThread) {
            lastChunk = null;
            lastPair = Long.MAX_VALUE;
            return chunks.isEmpty();
        }
        if (!submissions.isEmpty()) {
            if (aggressive) {
                pollSubmissions(0, aggressive);
            } else {
                pollSubmissions(Settings.settings().QUEUE.PARALLEL_THREADS, aggressive);
            }
        }
        synchronized (this) {
            return currentThread == null;
        }
    }

    /**
     * Get a new IChunk from either the pool, or create a new one<br> + Initialize it at the
     * coordinates
     *
     * @param chunkX
     * @param chunkZ
     * @return IChunk
     */
    private ChunkHolder poolOrCreate(int chunkX, int chunkZ) {
        ChunkHolder next = create(false);
        next.init(this, chunkX, chunkZ);
        return next;
    }

    @Override
    public final IQueueChunk getOrCreateChunk(int x, int z) {
        getChunkLock.lock();
        try {
            final long pair = (long) x << 32 | z & 0xffffffffL;
            if (pair == lastPair) {
                return lastChunk;
            }
            if (!processGet(x, z)) {
                lastPair = pair;
                lastChunk = NullChunk.getInstance();
                return NullChunk.getInstance();
            }
            IQueueChunk chunk = chunks.get(pair);
            if (chunk != null) {
                lastPair = pair;
                lastChunk = chunk;
                return chunk;
            }
            final int size = chunks.size();
            final boolean lowMem = MemUtil.isMemoryLimited();
            // If queueing is enabled AND either of the following
            //  - memory is low & queue size > num threads + 8
            //  - queue size > target size and primary queue has less than num threads submissions
            if (enabledQueue && ((lowMem && size > Settings.settings().QUEUE.PARALLEL_THREADS + 8) || (size > Settings.settings().QUEUE.TARGET_SIZE && Fawe
                    .instance()
                    .getQueueHandler()
                    .isUnderutilized()))) {
                chunk = chunks.removeFirst();
                final Future future = submitUnchecked(chunk);
                if (future != null && !future.isDone()) {
                    final int targetSize;
                    if (lowMem) {
                        targetSize = Settings.settings().QUEUE.PARALLEL_THREADS + 8;
                    } else {
                        targetSize = Settings.settings().QUEUE.TARGET_SIZE;
                    }
                    pollSubmissions(targetSize, lowMem);
                    submissions.add(future);
                }
            }
            chunk = poolOrCreate(x, z);
            chunk = wrap(chunk);

            chunks.put(pair, chunk);
            lastPair = pair;
            lastChunk = chunk;

            return chunk;
        } finally {
            getChunkLock.unlock();
        }
    }

    /**
     * Load a chunk in the world associated with this {@link SingleThreadQueueExtent} instance
     *
     * @param cx chunk X coordinate
     * @param cz chunk Z coordinate
     */
    public void addChunkLoad(int cx, int cz) {
        if (world == null) {
            return;
        }
        world.checkLoadedChunk(BlockVector3.at(cx << 4, 0, cz << 4));
    }

    /**
     * Define a region to be "preloaded" to the number of chunks provided by {@link Settings.QUEUE#PRELOAD_CHUNK_COUNT}
     *
     * @param region region of chunks
     */
    public void preload(Region region) {
        if (Settings.settings().QUEUE.PRELOAD_CHUNK_COUNT > 1) {
            int loadCount = 0;
            for (BlockVector2 from : region.getChunks()) {
                if (loadCount >= Settings.settings().QUEUE.PRELOAD_CHUNK_COUNT) {
                    break;
                }
                loadCount++;
                addChunkLoad(from.getBlockX(), from.getBlockZ());
            }
        }
    }

    @Override
    public ChunkHolder create(boolean isFull) {
        return ChunkHolder.newInstance();
    }

    private void pollSubmissions(int targetSize, boolean aggressive) {
        final int overflow = submissions.size() - targetSize;
        if (aggressive) {
            if (targetSize == 0) {
                while (!submissions.isEmpty()) {
                    iterateSubmissions();
                }
            }
            for (int i = 0; i < overflow; i++) {
                iterateSubmissions();
            }
        } else {
            for (int i = 0; i < overflow; i++) {
                Future next = submissions.peek();
                while (next != null) {
                    if (next.isDone()) {
                        Future after = null;
                        try {
                            after = (Future) next.get();
                        } catch (FaweException e) {
                            Fawe.handleFaweException(faweExceptionReasonsUsed, e, LOGGER);
                        } catch (ExecutionException | InterruptedException e) {
                            if (e.getCause() instanceof FaweException) {
                                Fawe.handleFaweException(faweExceptionReasonsUsed, (FaweException) e.getCause(), LOGGER);
                            } else {
                                String message = e.getMessage();
                                int hash = message.hashCode();
                                if (lastException != hash) {
                                    lastException = hash;
                                    exceptionCount = 0;
                                    LOGGER.catching(e);
                                } else if (exceptionCount < Settings.settings().QUEUE.PARALLEL_THREADS) {
                                    exceptionCount++;
                                    LOGGER.warn(message);
                                }
                            }
                        } finally {
                            /*
                             * If the execution failed, namely next.get() threw an exception,
                             * we don't want to process that Future again. Instead, we just drop
                             * it and set it to null, otherwise to the returned next Future.
                             */
                            next = after;
                        }
                    } else {
                        return;
                    }
                }
                submissions.poll();
            }
        }
    }

    private void iterateSubmissions() {
        Future first = submissions.poll();
        try {
            while (first != null) {
                first = (Future) first.get();
            }
        } catch (FaweException e) {
            Fawe.handleFaweException(faweExceptionReasonsUsed, e, LOGGER);
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof FaweException) {
                Fawe.handleFaweException(faweExceptionReasonsUsed, (FaweException) e.getCause(), LOGGER);
            } else {
                String message = e.getMessage();
                int hash = message.hashCode();
                if (lastException != hash) {
                    lastException = hash;
                    exceptionCount = 0;
                    LOGGER.catching(e);
                } else if (exceptionCount < Settings.settings().QUEUE.PARALLEL_THREADS) {
                    exceptionCount++;
                    LOGGER.warn(message);
                }
            }
        }
    }

    @Override
    public synchronized void flush() {
        if (!chunks.isEmpty()) {
            if (MemUtil.isMemoryLimited()) {
                for (IQueueChunk chunk : chunks.values()) {
                    final Future future = submitUnchecked(chunk);
                    if (future != null && !future.isDone()) {
                        pollSubmissions(Settings.settings().QUEUE.PARALLEL_THREADS, true);
                        submissions.add(future);
                    }
                }
            } else {
                for (IQueueChunk chunk : chunks.values()) {
                    final Future future = submitUnchecked(chunk);
                    if (future != null && !future.isDone()) {
                        submissions.add(future);
                    }
                }
            }
            getChunkLock.lock();
            chunks.clear();
            getChunkLock.unlock();
        }
        pollSubmissions(0, true);
    }

    @Override
    public ChunkFilterBlock initFilterBlock() {
        return new CharFilterBlock(this);
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.ADDING_BLOCKS;
    }

}
