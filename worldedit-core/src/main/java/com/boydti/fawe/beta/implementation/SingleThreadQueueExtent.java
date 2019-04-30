package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.holder.ReferenceChunk;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Single threaded implementation for IQueueExtent (still abstract)
 *  - Does not implement creation of chunks (that has to implemented by the platform e.g. Bukkit)
 *
 *  This queue is reusable {@link #init(WorldChunkCache)}
 */
public abstract class SingleThreadQueueExtent implements IQueueExtent {
    private WorldChunkCache cache;
    private Thread currentThread;

    /**
     * Safety check to ensure that the thread being used matches the one being initialized on
     *  - Can be removed later
     */
    private void checkThread() {
        if (Thread.currentThread() != currentThread && currentThread != null) {
            throw new UnsupportedOperationException("This class must be used from a single thread. Use multiple queues for concurrent operations");
        }
    }

    @Override
    public WorldChunkCache getCache() {
        return cache;
    }

    /**
     * Reset the queue
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
        if (cache != null) {
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

    @Override
    public Future<?> submit(final IChunk chunk) {
        if (chunk.isEmpty()) {
            CHUNK_POOL.add(chunk);
            return null;
        }
        if (Fawe.isMainThread()) {
            if (!chunk.applyAsync()) {
                chunk.applySync();
            }
            return null;
        }
        QueueHandler handler = Fawe.get().getQueueHandler();
        return handler.submit(chunk);
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        // TODO trim individial chunk sections
        CHUNK_POOL.clear();
        if (Thread.currentThread() == currentThread) {
            lastChunk = null;
            lastPair = Long.MAX_VALUE;
            return chunks.isEmpty();
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
        if (next == null) next = create(false);
        next.init(this, X, Z);
        return next;
    }

    @Override
    public final IChunk getCachedChunk(final int X, final int Z) {
        final long pair = (((long) X) << 32) | (Z & 0xffffffffL);
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
        if (size > Settings.IMP.QUEUE.TARGET_SIZE || MemUtil.isMemoryLimited()) {
            if (size > Settings.IMP.QUEUE.PARALLEL_THREADS * 2 + 16) {
                chunk = chunks.removeFirst();
                submit(chunk);
            }
        }
        chunk = poolOrCreate(X, Z);
        chunk = wrap(chunk);

        chunks.put(pair, chunk);
        lastPair = pair;
        lastChunk = chunk;

        return chunk;
    }

    @Override
    public synchronized void flush() {
        checkThread();
        if (!chunks.isEmpty()) {
            final Future[] tasks = new ForkJoinTask[chunks.size()];
            int i = 0;
            for (final IChunk chunk : chunks.values()) {
                tasks[i++] = submit(chunk);
            }
            chunks.clear();
            for (final Future task : tasks) {
                if (task != null) {
                    try {
                        task.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        reset();
    }
}