package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.holder.ReferenceChunk;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SingleThreadQueueExtent implements IQueueExtent {
    private WorldChunkCache cache;
    private Thread currentThread;

    private void checkThread() {
        if (Thread.currentThread() != currentThread && currentThread != null) {
            throw new UnsupportedOperationException("This class must be used from a single thread. Use multiple queues for concurrent operations");
        }
    }

    public WorldChunkCache getCache() {
        return cache;
    }

    protected synchronized void reset() {
        checkThread();
        cache = null;
        if (!chunks.isEmpty()) {
            for (IChunk chunk : chunks.values()) {
                chunk = chunk.getRoot();
                if (chunk != null) {
                    chunkPool.add(chunk);
                }
            }
            chunks.clear();
        }
        lastChunk = null;
        lastPair = Long.MAX_VALUE;
        currentThread = null;
    }

    @Override
    public synchronized void init(final WorldChunkCache cache) {
        if (cache != null) {
            reset();
        }
        currentThread = Thread.currentThread();
        checkNotNull(cache);
        this.cache = cache;
    }

    private IChunk lastChunk;
    private long lastPair = Long.MAX_VALUE;
    private final Long2ObjectLinkedOpenHashMap<IChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();
    private final ConcurrentLinkedQueue<IChunk> chunkPool = new ConcurrentLinkedQueue<>();

    @Override
    public <T> ForkJoinTask<T> submit(final IChunk<T, ?> tmp) {
        final ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
        return pool.submit(new Callable<T>() {
            @Override
            public T call() {
                IChunk<T, ?> chunk = tmp;

                T result = chunk.apply();

                chunk = chunk.getRoot();
                if (chunk != null) {
                    chunkPool.add(chunk);
                }
                return result;
            }
        });
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        chunkPool.clear();
        if (Thread.currentThread() == currentThread) {
            lastChunk = null;
            lastPair = Long.MAX_VALUE;
            return chunks.isEmpty();
        }
        synchronized (this) {
            return currentThread == null;
        }
    }

    private IChunk pool(final int X, final int Z) {
        IChunk next = chunkPool.poll();
        if (next == null) next = create(false);
        next.init(this, X, Z);
        return next;
    }

    public final IChunk getCachedChunk(final int X, final int Z) {
        final long pair = MathMan.pairInt(X, Z);
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
        chunk = pool(X, Z);
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
            final ForkJoinTask[] tasks = new ForkJoinTask[chunks.size()];
            int i = 0;
            for (final IChunk chunk : chunks.values()) {
                tasks[i++] = submit(chunk);
            }
            chunks.clear();
            for (final ForkJoinTask task : tasks) {
                if (task != null) task.join();
            }
        }
        reset();
    }
}