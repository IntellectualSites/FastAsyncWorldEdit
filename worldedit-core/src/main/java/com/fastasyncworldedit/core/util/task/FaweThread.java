package com.fastasyncworldedit.core.util.task;

import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface FaweThread {

    /**
     * Removes the extent currently associated with the calling thread.
     */
    void clearCurrentExtent();

    /**
     * Sets the extent associated with the calling thread.
     */
    void setCurrentExtent(Extent extent);

    /**
     * Gets the extent associated with the calling thread.
     */
    Extent getCurrentExtent();
}
