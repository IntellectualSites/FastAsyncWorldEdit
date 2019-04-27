package com.boydti.fawe.bukkit.v1_13.beta;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class QueueHandler {
    private SingleThreadQueueExtent mainExtent;
    private SingleThreadQueueExtent[] pool;



    public static void apply(Region region, Filter filter) { // TODO not MCAFilter, but another similar class
        // The chunks positions to iterate over
        Set<BlockVector2> chunks = region.getChunks();
        Iterator<BlockVector2> chunksIter = chunks.iterator();

        // Get a pool, to operate on the chunks in parallel
        ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
        int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
        ForkJoinTask[] tasks = new ForkJoinTask[size];

        for (int i = 0; i < size; i++) {
            tasks[i] = pool.submit(new Runnable() {
                @Override
                public void run() {
                    // Create a chunk that we will reuse/reset for each operation
                    IChunk chunk = create(true);

                    while (true) {
                        // Get the next chunk pos
                        BlockVector2 pos;
                        synchronized (chunksIter) {
                            if (!chunksIter.hasNext()) return;
                            pos = chunksIter.next();
                        }
                        int X = pos.getX();
                        int Z = pos.getZ();
                        long pair = MathMan.pairInt(X, Z);

                        // Initialize
                        chunk.init(SingleThreadQueueExtent.this, X, Z);

                        { // Start set
                            lastPair = pair;
                            lastChunk = chunk;
                        }
                        try {
                            if (!filter.appliesChunk(X, Z)) {
                                continue;
                            }
                            chunk = filter.applyChunk(chunk);

                            if (chunk == null) continue;

                            chunk.filter(filter);

                            filter.finishChunk(chunk);

                            chunk.apply();
                        } finally
                        { // End set
                            lastPair = Long.MAX_VALUE;
                            lastChunk = null;
                        }
                    }
                }
            });
        }

        // Join the tasks
        for (ForkJoinTask task : tasks) {
            task.join();
        }
    }
}