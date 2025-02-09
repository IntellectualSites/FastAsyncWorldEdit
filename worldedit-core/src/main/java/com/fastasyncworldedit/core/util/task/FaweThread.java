package com.fastasyncworldedit.core.util.task;

import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@ApiStatus.Internal
public class FaweThread extends ForkJoinWorkerThread {

    private static final ThreadLocal<Extent> EXTENTS = new ThreadLocal<>();

    private Extent currentExtent;

    protected FaweThread(final ForkJoinPool pool) {
        super(null, pool, true);
    }

    /**
     * Removes the extent currently associated with the calling thread.
     */
    public static void clearCurrentExtent() {
        if (Thread.currentThread() instanceof FaweThread ft) {
            ft.currentExtent = null;
        } else {
            EXTENTS.remove();
        }
    }

    /**
     * Sets the extent associated with the calling thread.
     */
    public static void setCurrentExtent(Extent extent) {
        if (Thread.currentThread() instanceof FaweThread ft) {
            ft.currentExtent = extent;
        } else {
            EXTENTS.set(extent);
        }

    }

    public static Extent getCurrentExtent() {
        if (Thread.currentThread() instanceof FaweThread ft) {
            return ft.currentExtent;
        } else {
            return EXTENTS.get();
        }
    }

}
