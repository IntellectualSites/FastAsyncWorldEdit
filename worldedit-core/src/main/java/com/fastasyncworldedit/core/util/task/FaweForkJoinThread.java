package com.fastasyncworldedit.core.util.task;

import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@ApiStatus.Internal
public class FaweForkJoinThread extends ForkJoinWorkerThread implements FaweThread {

    private Extent currentExtent;

    protected FaweForkJoinThread(final ForkJoinPool pool) {
        super(null, pool, true);
    }

    @Override
    public void clearCurrentExtent() {
        this.currentExtent = null;
    }

    @Override
    public void setCurrentExtent(final Extent extent) {
        this.currentExtent = extent;
    }

    @Override
    public Extent getCurrentExtent() {
        return this.currentExtent;
    }

}
