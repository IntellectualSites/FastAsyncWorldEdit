package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

/**
 * Class which handles all the queues {@link IQueueExtent}
 */
public abstract class QueueHandler implements Trimable {
    private Map<World, WeakReference<WorldChunkCache>> chunkCache = new HashMap<>();

    private IterableThreadLocal<IQueueExtent> pool = new IterableThreadLocal<IQueueExtent>() {
        @Override
        public IQueueExtent init() {
            return create();
        }
    };

    public Future<?> submit(IChunk chunk) {
        if (Fawe.isMainThread()) {
            if (!chunk.applyAsync()) {
                chunk.applySync();
            }
            return null;
        }
        // TODO return future
        return null;
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

    public IQueueExtent getQueue(World world) {
        IQueueExtent queue = pool.get();
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
        final ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
        final int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
        final ForkJoinTask[] tasks = new ForkJoinTask[size];

        for (int i = 0; i < size; i++) {
            tasks[i] = pool.submit(new Runnable() {
                @Override
                public void run() {
                    Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    IQueueExtent queue = getQueue(world);
                    FilterBlock block = null;

                    while (true) {
                        // Get the next chunk pos
                        final BlockVector2 pos;
                        synchronized (chunksIter) {
                            if (!chunksIter.hasNext()) return;
                            pos = chunksIter.next();
                        }
                        final int X = pos.getX();
                        final int Z = pos.getZ();
                        // TODO create full
                        IChunk chunk = queue.getCachedChunk(X, Z);
                        // Initialize
                        chunk.init(queue, X, Z);
                        try {
                            if (!newFilter.appliesChunk(X, Z)) {
                                continue;
                            }
                            chunk = newFilter.applyChunk(chunk);

                            if (chunk == null) continue;

                            if (block == null) block = queue.initFilterBlock();
                            chunk.filter(newFilter, block);

                            newFilter.finishChunk(chunk);

                            queue.submit(chunk);
                        } finally
                        {
                            if (filter != newFilter) {
                                synchronized (filter) {
                                    newFilter.join(filter);
                                }
                            }
                        }
                    }
                }
            });
        }

        // Join the tasks
        for (final ForkJoinTask task : tasks) {
            task.join();
        }
    }
}