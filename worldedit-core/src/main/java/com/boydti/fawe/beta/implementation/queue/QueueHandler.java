package com.boydti.fawe.beta.implementation.queue;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.beta.implementation.cache.ChunkCache;
import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.CleanableThreadLocal;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Class which handles all the queues {@link IQueueExtent}
 */
public abstract class QueueHandler implements Trimable, Runnable {
    private ForkJoinPool forkJoinPoolPrimary = new ForkJoinPool();
    private ForkJoinPool forkJoinPoolSecondary = new ForkJoinPool();
    private ThreadPoolExecutor blockingExecutor = FaweCache.IMP.newBlockingExecutor();
    private ConcurrentLinkedQueue<FutureTask> syncTasks = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<FutureTask> syncWhenFree = new ConcurrentLinkedQueue<>();

    private Map<World, WeakReference<IChunkCache<IChunkGet>>> chunkGetCache = new HashMap<>();
    private CleanableThreadLocal<IQueueExtent> queuePool = new CleanableThreadLocal<>(QueueHandler.this::create);
    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the
     * server
     */
    private long last;
    private long allocate = 50;
    private double targetTPS = 18;

    public QueueHandler() {
        TaskManager.IMP.repeat(this, 1);
    }

    @Override
    public void run() {
        if (!Fawe.isMainThread()) {
            throw new IllegalStateException("Not main thread");
        }
        if (!syncTasks.isEmpty()) {
            long currentAllocate = getAllocate();

            if (!MemUtil.isMemoryFree()) {
                // TODO reduce mem usage
                // FaweCache trim
                // Preloader trim
            }

            operate(syncTasks, last, currentAllocate);
        } else if (!syncWhenFree.isEmpty()) {
            operate(syncWhenFree, last, getAllocate());
        } else {
            // trim??
        }
    }

    public boolean isUnderutilized() {
        return blockingExecutor.getActiveCount() < blockingExecutor.getMaximumPoolSize();
    }

    private long getAllocate() {
        long now = System.currentTimeMillis();
        targetTPS = 18 - Math.max(Settings.IMP.QUEUE.EXTRA_TIME_MS * 0.05, 0);
        long diff = 50 + this.last - (this.last = now);
        long absDiff = Math.abs(diff);
        if (diff == 0) {
            allocate = Math.min(50, allocate + 1);
        } else if (diff < 0) {
            allocate = Math.max(5, allocate + diff);
        } else if (!Fawe.get().getTimer().isAbove(targetTPS)) {
            allocate = Math.max(5, allocate - 1);
        }
        return allocate - absDiff;
    }

    private void operate(Queue<FutureTask> queue, long start, long currentAllocate) {
        boolean wait = false;
        do {
            Runnable task = queue.poll();
            if (task == null) {
                if (wait) {
                    synchronized (syncTasks) {
                        try {
                            queue.wait(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    task = queue.poll();
                    wait = false;
                } else {
                    break;
                }
            }
            if (task != null) {
                task.run();
                wait = true;
            }
        } while (System.currentTimeMillis() - start < currentAllocate);
    }

    public <T> Future<T> async(final Runnable run, final T value) {
        return forkJoinPoolSecondary.submit(run, value);
    }

    public <T> Future<T> async(final Callable<T> call) {
        return forkJoinPoolSecondary.submit(call);
    }

    public ForkJoinTask submit(Runnable call) {
        return forkJoinPoolPrimary.submit(call);
    }

    public <T> Future<T> sync(Runnable run, T value) {
        return sync(run, value, syncTasks);
    }

    public <T> Future<T> sync(Runnable run) {
        return sync(run, syncTasks);
    }

    public <T> Future<T> sync(Callable<T> call) throws Exception {
        return sync(call, syncTasks);
    }

    public <T> Future<T> sync(Supplier<T> call) {
        return sync(call, syncTasks);
    }

    // Lower priorty sync task (runs only when there are no other tasks)
    public <T> Future<T> syncWhenFree(Runnable run, T value) {
        return sync(run, value, syncWhenFree);
    }

    public <T> Future<T> syncWhenFree(Runnable run) {
        return sync(run, syncWhenFree);
    }

    public <T> Future<T> syncWhenFree(Callable<T> call) throws Exception {
        return sync(call, syncWhenFree);
    }

    public <T> Future<T> syncWhenFree(Supplier<T> call) {
        return sync(call, syncWhenFree);
    }

    private <T> Future<T> sync(Runnable run, T value, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateFuture(value);
        }
        final FutureTask<T> result = new FutureTask<>(run, value);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Runnable run, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateCancelledFuture();
        }
        final FutureTask<T> result = new FutureTask<>(run, null);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Callable<T> call, Queue<FutureTask> queue) throws Exception {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.call());
        }
        final FutureTask<T> result = new FutureTask<>(call);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Supplier<T> call, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.get());
        }
        final FutureTask<T> result = new FutureTask<>(call::get);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private void notifySync(Object object) {
        synchronized (object) {
            object.notifyAll();
        }
    }

    public <T extends Future<T>> T submit(IQueueChunk<T> chunk) {
//        if (MemUtil.isMemoryFree()) { TODO NOT IMPLEMENTED - optimize this
//            return (T) forkJoinPoolSecondary.submit(chunk);
        }
        return (T) blockingExecutor.submit(chunk);
    }

    /**
     * Get or create the WorldChunkCache for a world
     * @param world
     * @return
     */
    public WorldChunkCache getOrCreate(World world) {
        world = WorldWrapper.unwrap(world);

        synchronized (chunkGetCache) {
            final WeakReference<IChunkCache<IChunkGet>> ref = chunkGetCache.get(world);
            if (ref != null) {
                final WorldChunkCache cached = ref.get();
                if (cached != null) {
                    return cached;
                }
            }
            final IChunkCache<IChunkGet> created = new ChunkCache<>(world);
            chunkGetCache.put(world, new WeakReference<>(created));
            return created;
        }
    }

    public IQueueExtent create() {
        return new SingleThreadQueueExtent();
    }

    public void uncache() {
        queuePool.set(null);
    }

    private IQueueExtent pool() {
        IQueueExtent queue = queuePool.get();
        if (queue == null) {
            queuePool.set(queue = queuePool.init());
        }
        return queue;
    }

    public abstract void startSet(boolean parallel);

    public abstract void endSet(boolean parallel);

    public IQueueExtent getQueue(World world) {
        return getQueue(world, null);
    }

    public IQueueExtent getQueue(World world, IBatchProcessor processor) {
        final IQueueExtent queue = pool();
        IChunkCache<IChunkGet> cacheGet = getOrCreateWorldCache(world);
        IChunkCache<IChunkSet> set = null; // TODO cache?
        queue.init(world, cacheGet, set);
        if (processor != null) {
            queue.setProcessor(processor);
        }
        return queue;
    }

    @Override
    public boolean trim(final boolean aggressive) {
        boolean result = true;
        synchronized (chunkGetCache) {
            final Iterator<Map.Entry<World, WeakReference<IChunkCache<IChunkGet>>>> iter = chunkGetCache
                .entrySet().iterator();
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

    public void apply(final World world, final Region region, final Filter filter) {
        // The chunks positions to iterate over
        final Set<BlockVector2> chunks = region.getChunks();
        final Iterator<BlockVector2> chunksIter = chunks.iterator();

        // Get a pool, to operate on the chunks in parallel
        final int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
        final ForkJoinTask[] tasks = new ForkJoinTask[size];
        for (int i = 0; i < size; i++) {
            tasks[i] = forkJoinPoolPrimary.submit(new Runnable() {
                @Override
                public void run() {
                    final Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    final IQueueExtent queue = getQueue(world);
                    synchronized (queue) {
                        ChunkFilterBlock block = null;

                        while (true) {
                            // Get the next chunk posWeakChunk
                            final int X, Z;
                            synchronized (chunksIter) {
                                if (!chunksIter.hasNext()) break;
                                final BlockVector2 pos = chunksIter.next();
                                X = pos.getX();
                                Z = pos.getZ();
                            }
                            if (!newFilter.appliesChunk(X, Z)) {
                                continue;
                            }
                            IChunk chunk = queue.getCachedChunk(X, Z);
                            // Initialize
                            chunk.init(queue, X, Z);

                            IChunk newChunk = newFilter.applyChunk(chunk, region);
                            if (newChunk != null) {
                                chunk = newChunk;
                                if (block == null) block = queue.initFilterBlock();
                                chunk.filterBlocks(newFilter, block, region);
                            }
                            queue.submit(chunk);
                        }
                        queue.flush();
                    }
                }
            });
        }
        // Join filters
        for (int i = 0; i < tasks.length; i++) {
            final ForkJoinTask task = tasks[i];
            if (task != null) {
                task.quietlyJoin();
            }
        }
        filter.join();
    }
}
