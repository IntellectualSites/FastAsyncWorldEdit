package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    public static void apply(final Region region, final Filter filter) { // TODO not MCAFilter, but another similar class
//        // The chunks positions to iterate over
//        final Set<BlockVector2> chunks = region.getChunks();
//        final Iterator<BlockVector2> chunksIter = chunks.iterator();
//
//        // Get a pool, to operate on the chunks in parallel
//        final ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
//        final int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
//        final ForkJoinTask[] tasks = new ForkJoinTask[size];
//
//        for (int i = 0; i < size; i++) {
//            tasks[i] = pool.submit(new Runnable() {
//                @Override
//                public void run() {
//                    // Create a chunk that we will reuse/reset for each operation
//                    IChunk chunk = create(true);
//
//                    while (true) {
//                        // Get the next chunk pos
//                        final BlockVector2 pos;
//                        synchronized (chunksIter) {
//                            if (!chunksIter.hasNext()) return;
//                            pos = chunksIter.next();
//                        }
//                        final int X = pos.getX();
//                        final int Z = pos.getZ();
//                        final long pair = MathMan.pairInt(X, Z);
//
//                        // Initialize
//                        chunk.init(SingleThreadQueueExtent.this, X, Z);
//
//                        { // Start set
//                            lastPair = pair;
//                            lastChunk = chunk;
//                        }
//                        try {
//                            if (!filter.appliesChunk(X, Z)) {
//                                continue;
//                            }
//                            chunk = filter.applyChunk(chunk);
//
//                            if (chunk == null) continue;
//
//                            chunk.filter(filter);
//
//                            filter.finishChunk(chunk);
//
//                            chunk.apply();
//                        } finally
//                        { // End set
//                            lastPair = Long.MAX_VALUE;
//                            lastChunk = null;
//                        }
//                    }
//                }
//            });
//        }
//
//        // Join the tasks
//        for (final ForkJoinTask task : tasks) {
//            task.join();
//        }
    }
}