package com.boydti.fawe.beta.implementation.queue;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.blocks.CharSetBlocks;
import com.boydti.fawe.beta.implementation.chunk.ChunkHolder;
import com.boydti.fawe.beta.implementation.chunk.NullChunk;
import com.boydti.fawe.beta.implementation.chunk.ReferenceChunk;
import com.boydti.fawe.beta.implementation.filter.block.CharFilterBlock;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.implementation.processors.EmptyBatchProcessor;
import com.boydti.fawe.beta.implementation.processors.ExtentBatchProcessorHolder;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.google.common.util.concurrent.Futures;
import com.sk89q.worldedit.extent.Extent;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Single threaded implementation for IQueueExtent (still abstract) - Does not implement creation of
 * chunks (that has to implemented by the platform e.g., Bukkit)
 * <p>
 * This queue is reusable {@link #init(Extent, IChunkCache, IChunkCache)} }
 */
public class SingleThreadQueueExtent extends ExtentBatchProcessorHolder implements IQueueExtent<IQueueChunk> {

//    // Pool discarded chunks for reuse (can safely be cleared by another thread)
//    private static final ConcurrentLinkedQueue<IChunk> CHUNK_POOL = new ConcurrentLinkedQueue<>();
    // Chunks currently being queued / worked on
    private final Long2ObjectLinkedOpenHashMap<IQueueChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();

    private IChunkCache<IChunkGet> cacheGet;
    private IChunkCache<IChunkSet> cacheSet;
    private boolean initialized;

    private Thread currentThread;
    private ConcurrentLinkedQueue<Future> submissions = new ConcurrentLinkedQueue<>();
    // Last access pointers
    private IQueueChunk lastChunk;
    private long lastPair = Long.MAX_VALUE;

    private boolean enabledQueue = true;

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
    public IChunkGet getCachedGet(int x, int z) {
        return cacheGet.get(x, z);
    }

    @Override
    public IChunkSet getCachedSet(int x, int z) {
        return cacheSet.get(x, z);
    }

    /**
     * Resets the queue.
     */
    protected synchronized void reset() {
        if (!this.initialized) return;
        if (!this.chunks.isEmpty()) {
            for (IChunk chunk : this.chunks.values()) {
                chunk.recycle();
            }
            this.chunks.clear();
        }
        this.enabledQueue = true;
        this.lastChunk = null;
        this.lastPair = Long.MAX_VALUE;
        this.currentThread = null;
        this.initialized = false;
        this.setProcessor(EmptyBatchProcessor.INSTANCE);
    }

    /**
     * Initialize the queue
     *
     */
    @Override
    public synchronized void init(Extent extent, IChunkCache<IChunkGet> get, IChunkCache<IChunkSet> set) {
        reset();
        currentThread = Thread.currentThread();
        if (get == null) {
            get = (x, z) -> { throw new UnsupportedOperationException(); };
        }
        if (set == null) {
            set = (x, z) -> CharSetBlocks.newInstance();
        }
        this.cacheGet = get;
        this.cacheSet = set;
        this.setProcessor(EmptyBatchProcessor.INSTANCE);
        initialized = true;
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
        chunks.remove(index, chunk);
        return submitUnchecked(chunk);
    }

    /**
     * Submit without first checking that it has been removed from the chunk map
     *
     * @param chunk
     * @return
     */
    private <V extends Future<V>> V submitUnchecked(IQueueChunk chunk) {
        if (chunk.isEmpty()) {
            chunk.recycle();
            Future result = Futures.immediateFuture(null);
            return (V) result;
        }

        if (Fawe.isMainThread()) {
            return (V) chunk.call();
        }

        return (V) Fawe.get().getQueueHandler().submit(chunk);
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
                pollSubmissions(Settings.IMP.QUEUE.PARALLEL_THREADS, aggressive);
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
     * @param X
     * @param Z
     * @return IChunk
     */
    private ChunkHolder poolOrCreate(int X, int Z) {
        ChunkHolder next = create(false);
        next.init(this, X, Z);
        return next;
    }

    @Override
    public final IQueueChunk getOrCreateChunk(int x, int z) {
        final long pair = (long) x << 32 | z & 0xffffffffL;
        if (pair == lastPair) {
            return lastChunk;
        }
        if (!processGet(x, z)) {
            lastPair = pair;
            lastChunk = NullChunk.INSTANCE;
            return NullChunk.INSTANCE;
        }

        IQueueChunk chunk = chunks.get(pair);
        if (chunk instanceof ReferenceChunk) {
            chunk = ((ReferenceChunk) chunk).getParent();
        }
        if (chunk != null) {
            lastPair = pair;
            lastChunk = chunk;
        }
        if (chunk != null) {
            return chunk;
        }
        final int size = chunks.size();
        final boolean lowMem = MemUtil.isMemoryLimited();
        if (enabledQueue && (lowMem || size > Settings.IMP.QUEUE.TARGET_SIZE)) {
            chunk = chunks.removeFirst();
            final Future future = submitUnchecked(chunk);
            if (future != null && !future.isDone()) {
                final int targetSize;
                if (lowMem) {
                    targetSize = Settings.IMP.QUEUE.PARALLEL_THREADS;
                } else {
                    targetSize = Settings.IMP.QUEUE.TARGET_SIZE;
                }
                pollSubmissions(targetSize, true);
                submissions.add(future);
            }
        }
        chunk = poolOrCreate(x, z);
        chunk = wrap(chunk);

        chunks.put(pair, chunk);
        lastPair = pair;
        lastChunk = chunk;

        return chunk;
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
                    Future future = submissions.poll();
                    try {
                        while (future != null) future = (Future) future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
            for (int i = 0; i < overflow; i++) {
                Future first = submissions.poll();
                try {
                    while (first != null) first = (Future) first.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (int i = 0; i < overflow; i++) {
                Future next = submissions.peek();
                while (next != null) {
                    if (next.isDone()) {
                        try {
                            next = (Future) next.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        return;
                    }
                }
                submissions.poll();
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
                        pollSubmissions(Settings.IMP.QUEUE.PARALLEL_THREADS, true);
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
            chunks.clear();
        }
        pollSubmissions(0, true);
    }

    @Override
    public ChunkFilterBlock initFilterBlock() {
        return new CharFilterBlock(this);
    }
}
