package com.boydti.fawe.beta.implementation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.holder.ReferenceChunk;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.google.common.util.concurrent.Futures;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Single threaded implementation for IQueueExtent (still abstract)
 *  - Does not implement creation of chunks (that has to implemented by the platform e.g. Bukkit)
 *
 *  This queue is reusable {@link #init(WorldChunkCache)}
 */
public abstract class SingleThreadQueueExtent implements IQueueExtent {
    private WorldChunkCache cache;
    private Thread currentThread;
    private ConcurrentLinkedQueue<Future> submissions = new ConcurrentLinkedQueue<>();

    /**
     * Safety check to ensure that the thread being used matches the one being initialized on.
     *  - Can be removed later
     */
    private void checkThread() {
        if (Thread.currentThread() != currentThread && currentThread != null) {
            throw new UnsupportedOperationException("This class must be used from a single thread. Use multiple queues for concurrent operations");
        }
    }

    @Override
    public IChunkGet getCachedGet(int x, int z, Supplier<IChunkGet> supplier) {
        return cache.get(MathMan.pairInt(x, z), supplier);
    }

    /**
     * Resets the queue.
     */
    protected synchronized void reset() {
        checkThread();
        cache = null;
        if (!chunks.isEmpty()) {
            CHUNK_POOL.addAll(chunks.values());
            chunks.clear();
        }
        lastChunk = null;
        lastPair = Long.MAX_VALUE;
        currentThread = null;
    }

    /**
     * Initialize the queue
     * @param cache
     */
    @Override
    public synchronized void init(final WorldChunkCache cache) {
        if (this.cache != null) {
            reset();
        }
        currentThread = Thread.currentThread();
        checkNotNull(cache);
        this.cache = cache;
    }

    // Last access pointers
    private IChunk lastChunk;
    private long lastPair = Long.MAX_VALUE;
    // Chunks currently being queued / worked on
    private final Long2ObjectLinkedOpenHashMap<IChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();
    // Pool discarded chunks for reuse (can safely be cleared by another thread)
    private static final ConcurrentLinkedQueue<IChunk> CHUNK_POOL = new ConcurrentLinkedQueue<>();

    public void returnToPool(final IChunk chunk) {
        CHUNK_POOL.add(chunk);
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
    public <T extends Future<T>> T submit(final IChunk<T> chunk) {
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
     * @param chunk
     * @param <T>
     * @return
     */
    private <T extends Future<T>> T submitUnchecked(final IChunk<T> chunk) {
        if (chunk.isEmpty()) {
            CHUNK_POOL.add(chunk);
            return (T) (Future) Futures.immediateFuture(null);
        }

        if (Fawe.isMainThread()) {
            return chunk.call();
        }

        return Fawe.get().getQueueHandler().submit(chunk);
    }

    @Override
    public synchronized boolean trim(final boolean aggressive) {
        // TODO trim individial chunk sections
        CHUNK_POOL.clear();
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
     * Get a new IChunk from either the pool, or create a new one<br>
     *     + Initialize it at the coordinates
     * @param X
     * @param Z
     * @return IChunk
     */
    private IChunk poolOrCreate(final int X, final int Z) {
        IChunk next = CHUNK_POOL.poll();
        if (next == null) {
            next = create(false);
        }
        next.init(this, X, Z);
        return next;
    }

    @Override
    public final IChunk getCachedChunk(final int x, final int z) {
        final long pair = (((long) x) << 32) | (z & 0xffffffffL);
        if (pair == lastPair) {
            return lastChunk;
        }

        IChunk chunk = chunks.get(pair);
        if (chunk instanceof ReferenceChunk) {
            chunk = ((ReferenceChunk) (chunk)).getParent();
        }
        if (chunk != null) {
            lastPair = pair;
            lastChunk = chunk;
        }
        if (chunk != null) return chunk;

        checkThread();
        final int size = chunks.size();
        final boolean lowMem = MemUtil.isMemoryLimited();
        if (lowMem || size > Settings.IMP.QUEUE.TARGET_SIZE) {
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

    private void pollSubmissions(final int targetSize, final boolean aggressive) {
        final int overflow = submissions.size() - targetSize;
        if (aggressive) {
            for (int i = 0; i < overflow; i++) {
                Future first = submissions.poll();
                try {
                    while ((first = (Future) first.get()) != null) ;
                } catch (final InterruptedException | ExecutionException e) {
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
                        } catch (final InterruptedException | ExecutionException e) {
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
        checkThread();
        if (!chunks.isEmpty()) {
            if (MemUtil.isMemoryLimited()) {
                for (final IChunk chunk : chunks.values()) {
                    final Future future = submitUnchecked(chunk);
                    if (future != null && !future.isDone()) {
                        pollSubmissions(Settings.IMP.QUEUE.PARALLEL_THREADS, true);
                        submissions.add(future);
                    }
                }
            } else {
                for (final IChunk chunk : chunks.values()) {
                    final Future future = submitUnchecked(chunk);
                    if (future != null && !future.isDone()) {
                        submissions.add(future);
                    }
                }
            }
            chunks.clear();
        }
        pollSubmissions(0, true);
        reset();
    }
}
