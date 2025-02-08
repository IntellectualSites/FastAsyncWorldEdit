package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

class ApplyTask<F extends Filter> extends RecursiveAction implements Runnable {

    private static final int INITIAL_REGION_SHIFT = 5;
    private static final int SHIFT_REDUCTION = 2;

    private final CommonState<F> commonState;
    private final ApplyTask<F> before;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;
    // Note: shift == INITIAL_REGION_SHIFT means we are in the root node.
    // compute() relies on that when triggering postProcess
    private final int shift;

    @Override
    public void run() {
        compute();
    }

    private record CommonState<F extends Filter>(
            F originalFilter,
            Region region,
            ParallelQueueExtent parallelQueueExtent,
            ConcurrentMap<Thread, ThreadState<F>> stateCache,
            boolean full
    ) {

    }

    private static final class ThreadState<F extends Filter> {

        private final SingleThreadQueueExtent queue;
        private final F filter;
        private ChunkFilterBlock block;

        private ThreadState(SingleThreadQueueExtent queue, F filter) {
            this.queue = queue;
            this.filter = filter;
        }

    }

    ApplyTask(
            final Region region,
            final F filter,
            final ParallelQueueExtent parallelQueueExtent,
            final boolean full
    ) {
        this.commonState = new CommonState<>(
                filter,
                region.clone(), // clone only once, assuming the filter doesn't modify that clone
                parallelQueueExtent,
                new ConcurrentHashMap<>(),
                full
        );
        this.before = null;
        final BlockVector3 minimumPoint = region.getMinimumPoint();
        this.minChunkX = minimumPoint.x() >> 4;
        this.minChunkZ = minimumPoint.z() >> 4;
        final BlockVector3 maximumPoint = region.getMaximumPoint();
        this.maxChunkX = maximumPoint.x() >> 4;
        this.maxChunkZ = maximumPoint.z() >> 4;
        this.shift = INITIAL_REGION_SHIFT;

    }

    private ApplyTask(
            final CommonState<F> commonState,
            final ApplyTask<F> before,
            final int minChunkX,
            final int maxChunkX,
            final int minChunkZ,
            final int maxChunkZ,
            final int higherShift
    ) {
        this.commonState = commonState;
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkZ = maxChunkZ;
        this.before = before;
        this.shift = Math.max(0, higherShift - SHIFT_REDUCTION);
    }

    @Override
    protected void compute() {
        ApplyTask<F> subtask = null;
        if (this.minChunkX != this.maxChunkX || this.minChunkZ != this.maxChunkZ) {
            int minRegionX = this.minChunkX >> this.shift;
            int minRegionZ = this.minChunkZ >> this.shift;
            int maxRegionX = this.maxChunkX >> this.shift;
            int maxRegionZ = this.maxChunkZ >> this.shift;
            // This task covers multiple regions. Create one subtask per region
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    if (ForkJoinTask.getSurplusQueuedTaskCount() > Settings.settings().QUEUE.PARALLEL_THREADS) {
                        // assume we can do a bigger batch of work here - the other threads are busy for a while
                        processRegion(regionX, regionZ, this.shift);
                        continue;
                    }
                    if (this.shift == 0 && !this.commonState.region.containsChunk(regionX, regionZ)) {
                        // if shift == 0, region coords are chunk coords
                        continue; // chunks not intersecting with the region don't need a task
                    }

                    // creating more tasks will likely help parallelism as other threads aren't *that* busy
                    subtask = new ApplyTask<>(
                            this.commonState,
                            subtask,
                            regionX << this.shift,
                            ((regionX + 1) << this.shift) - 1,
                            regionZ << this.shift,
                            ((regionZ + 1) << this.shift) - 1,
                            this.shift
                    );
                    subtask.fork();
                }
            }
        } else {
            // we reached a task for a single chunk, let's process it
            processChunk(this.minChunkX, this.minChunkZ);
        }
        // try processing tasks in reverse order if not processed already, otherwise "wait" for completion
        while (subtask != null) {
            if (subtask.tryUnfork()) {
                subtask.invoke();
            } else {
                subtask.quietlyJoin();
            }
            subtask = subtask.before;
        }
        if (this.shift == INITIAL_REGION_SHIFT) {
            onCompletion();
        }
    }

    private void processRegion(int regionX, int regionZ, int shift) {
        final ThreadState<F> state = getState();
        this.commonState.parallelQueueExtent.enter(state.queue);
        try {
            for (int chunkX = regionX << shift; chunkX <= ((regionX  + 1) << shift) - 1; chunkX++) {
                for (int chunkZ = regionZ << shift; chunkZ <= ((regionZ  + 1) << shift) - 1; chunkZ++) {
                    if (!this.commonState.region.containsChunk(chunkX, chunkZ)) {
                        continue; // chunks not intersecting with the region must not be processed
                    }
                    applyChunk(chunkX, chunkZ, state);
                }
            }
        } finally {
            this.commonState.parallelQueueExtent.exit();
        }

    }

    @SuppressWarnings("unchecked")
    private ThreadState<F> getState() {
        return this.commonState.stateCache.computeIfAbsent(
                Thread.currentThread(),
                __ -> new ThreadState<>(
                        (SingleThreadQueueExtent) this.commonState.parallelQueueExtent.getNewQueue(),
                        (F) this.commonState.originalFilter.fork()
                )
        );
    }

    private void processChunk(int chunkX, int chunkZ) {
        final ThreadState<F> state = getState();
        this.commonState.parallelQueueExtent.enter(state.queue);
        try {
            applyChunk(chunkX, chunkZ, state);
        } finally {
            this.commonState.parallelQueueExtent.exit();
        }
    }

    private void applyChunk(int chunkX, int chunkZ, ThreadState<F> state) {
        state.block = state.queue.apply(
                state.block,
                state.filter,
                this.commonState.region,
                chunkX,
                chunkZ,
                this.commonState.full
        );
    }

    private void onCompletion() {
        for (ForkJoinTask<?> task : postProcess()) {
            if (task.tryUnfork()) {
                task.invoke();
            } else {
                task.quietlyJoin();
            }
        }
    }

    private ForkJoinTask<?>[] postProcess() {
        final Collection<ThreadState<F>> values = this.commonState.stateCache.values();
        ForkJoinTask<?>[] tasks = new ForkJoinTask[values.size()];
        int i = values.size() - 1;
        for (final ThreadState<F> value : values) {
            tasks[i] = ForkJoinTask.adapt(value.queue::flush).fork();
            i--;
        }
        return tasks;
    }

}
