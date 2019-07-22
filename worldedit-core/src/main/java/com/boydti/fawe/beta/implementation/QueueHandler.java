package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.google.common.util.concurrent.Futures;
import com.sk89q.worldedit.world.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * Class which handles all the queues {@link IQueueExtent}
 */
public abstract class QueueHandler implements Trimable, Runnable {
    private ForkJoinPool forkJoinPoolPrimary = new ForkJoinPool();
    private ForkJoinPool forkJoinPoolSecondary = new ForkJoinPool();
    private ThreadPoolExecutor blockingExecutor = FaweCache.newBlockingExecutor();
    private ConcurrentLinkedQueue<FutureTask> syncTasks = new ConcurrentLinkedQueue();

    private Map<World, WeakReference<WorldChunkCache>> chunkCache = new HashMap<>();
    private IterableThreadLocal<IQueueExtent> queuePool = new IterableThreadLocal<IQueueExtent>() {
        @Override
        public IQueueExtent init() {
            return create();
        }
    };

    public QueueHandler() {
        TaskManager.IMP.repeat(this, 1);
    }

    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the server
     */
    private long last;
    private long allocate = 50;
    private double targetTPS = 18;

    @Override
    public void run() {
        if (!Fawe.isMainThread()) {
            throw new IllegalStateException("Not main thread");
        }
        if (!syncTasks.isEmpty()) {
            long now = System.currentTimeMillis();
            targetTPS = 18 - Math.max(Settings.IMP.QUEUE.EXTRA_TIME_MS * 0.05, 0);
            long diff = (50 + this.last) - (this.last = now);
            long absDiff = Math.abs(diff);
            if (diff == 0) {
                allocate = Math.min(50, allocate + 1);
            } else if (diff < 0) {
                allocate = Math.max(5, allocate + diff);
            } else if (!Fawe.get().getTimer().isAbove(targetTPS)) {
                allocate = Math.max(5, allocate - 1);
            }
            long currentAllocate = allocate - absDiff;

            if (!MemUtil.isMemoryFree()) {
                // TODO reduce mem usage
            }

            long taskAllocate = currentAllocate;
            boolean wait = false;
            do {
                Runnable task = syncTasks.poll();
                if (task == null) {
                    if (wait) {
                        synchronized (syncTasks) {
                            try {
                                syncTasks.wait(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        task = syncTasks.poll();
                        wait = false;
                    } else {
                        break;
                    }
                }
                if (task != null) {
                    task.run();
                    wait = true;
                }
            } while (System.currentTimeMillis() - now < taskAllocate);
        }
        while (!syncTasks.isEmpty()) {
            final FutureTask task = syncTasks.poll();
            if (task != null) task.run();
        }
    }

    public <T extends Future<T>> void complete(Future<T> task) {
        try {
            while (task != null) {
                task = task.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public <T> Future<T> async(final Runnable run, final T value) {
        return forkJoinPoolSecondary.submit(run, value);
    }

    public Future<?> async(final Runnable run) {
        return forkJoinPoolSecondary.submit(run);
    }

    public <T> Future<T> async(final Callable<T> call) {
        return forkJoinPoolSecondary.submit(call);
    }

    public ForkJoinTask submit(final Runnable call) {
        return forkJoinPoolPrimary.submit(call);
    }

    public <T> Future<T> sync(final Runnable run, final T value) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateFuture(value);
        }
        final FutureTask<T> result = new FutureTask<>(run, value);
        syncTasks.add(result);
        notifySync();
        return result;
    }

    public <T> Future<T> sync(final Runnable run) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateCancelledFuture();
        }
        final FutureTask<T> result = new FutureTask<>(run, null);
        syncTasks.add(result);
        notifySync();
        return result;
    }

    public <T> Future<T> sync(final Callable<T> call) throws Exception {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.call());
        }
        final FutureTask<T> result = new FutureTask<>(call);
        syncTasks.add(result);
        notifySync();
        return result;
    }

    public <T> Future<T> sync(final Supplier<T> call) {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.get());
        }
        final FutureTask<T> result = new FutureTask<>(call::get);
        syncTasks.add(result);
        notifySync();
        return result;
    }

    private void notifySync() {
        synchronized (syncTasks) {
            syncTasks.notifyAll();
        }
    }

    public <T extends Future<T>> T submit(final IChunk<T> chunk) {
//        if (MemUtil.isMemoryFree()) { TODO NOT IMPLEMENTED - optimize this
//            return (T) forkJoinPoolSecondary.submit(chunk);
//        }
        return (T) blockingExecutor.submit(chunk);
    }

    /**
     * Get or create the WorldChunkCache for a world
     * @param world
     * @return
     */
    public WorldChunkCache getOrCreate(World world) {
        world = WorldWrapper.unwrap(world);

        synchronized (chunkCache) {
            final WeakReference<WorldChunkCache> ref = chunkCache.get(world);
            if (ref != null) {
                final WorldChunkCache cached = ref.get();
                if (cached != null) {
                    return cached;
                }
            }
            final WorldChunkCache created = new WorldChunkCache(world);
            chunkCache.put(world, new WeakReference<>(created));
            return created;
        }
    }

    public abstract IQueueExtent create();

    public abstract void startSet(boolean parallel);

    public abstract void endSet(boolean parallel);

    public IQueueExtent getQueue(final World world) {
        final IQueueExtent queue = queuePool.get();
        queue.init(getOrCreate(world));
        return queue;
    }

    @Override
    public boolean trim(final boolean aggressive) {
        boolean result = true;
        synchronized (chunkCache) {
            final Iterator<Map.Entry<World, WeakReference<WorldChunkCache>>> iter = chunkCache.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<World, WeakReference<WorldChunkCache>> entry = iter.next();
                final WeakReference<WorldChunkCache> value = entry.getValue();
                final WorldChunkCache cache = value.get();
                if (cache == null || cache.size() == 0 || cache.trim(aggressive)) {
                    iter.remove();
                    continue;
                }
                result = false;
            }
        }
        return result;
    }
}