package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

    @Override
    public void run() {
        if (!Fawe.isMainThread()) {
            throw new IllegalStateException("Not main thread");
        }
        while (!syncTasks.isEmpty()) {
            final FutureTask task = syncTasks.poll();
            if (task != null) task.run();
        }
    }

    public <T> Future<T> async(final Runnable run, final T value) {
        return forkJoinPoolSecondary.submit(run, value);
    }

    public <T> Future<T> async(final Callable<T> call) {
        return forkJoinPoolSecondary.submit(call);
    }

    public <T> Future<T> sync(final Runnable run, final T value) {
        final FutureTask<T> result = new FutureTask<>(run, value);
        syncTasks.add(result);
        return result;
    }

    public <T> Future<T> sync(final Runnable run) {
        final FutureTask<T> result = new FutureTask<>(run, null);
        syncTasks.add(result);
        return result;
    }

    public <T> Future<T> sync(final Callable<T> call) {
        final FutureTask<T> result = new FutureTask<>(call);
        syncTasks.add(result);
        return result;
    }

    public <T extends Future<T>> T submit(final IChunk<T> chunk) {
        if (MemUtil.isMemoryFree()) {
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
                    MutableBlockVector3 mbv1 = new MutableBlockVector3();
                    MutableBlockVector3 mbv2 = new MutableBlockVector3();

                    final Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    final IQueueExtent queue = getQueue(world);
                    synchronized (queue) {
                        FilterBlock block = null;

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

                            chunk = newFilter.applyChunk(chunk, region);

                            if (chunk == null) continue;

                            if (block == null) block = queue.initFilterBlock();
                            chunk.filter(newFilter, block, region, mbv1, mbv2);

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