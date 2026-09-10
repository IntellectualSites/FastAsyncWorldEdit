package com.fastasyncworldedit.core.util.task;

import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class FaweThreadUtil {

    private static final ThreadLocal<Extent> EXTENTS = new ThreadLocal<>();

    private FaweThreadUtil() {}

    /**
     * Removes the extent currently associated with the calling thread.
     */
    public static void clearCurrentExtent() {
        if (Thread.currentThread() instanceof FaweThread ft) {
            ft.clearCurrentExtent();
        } else {
            EXTENTS.remove();
        }
    }

    /**
     * Sets the extent associated with the calling thread.
     */
    public static void setCurrentExtent(Extent extent) {
        if (Thread.currentThread() instanceof FaweForkJoinThread ft) {
            ft.setCurrentExtent(extent);
        } else {
            EXTENTS.set(extent);
        }

    }

    public static Extent getCurrentExtent() {
        if (Thread.currentThread() instanceof FaweForkJoinThread ft) {
            return ft.getCurrentExtent();
        } else {
            return EXTENTS.get();
        }
    }


}
