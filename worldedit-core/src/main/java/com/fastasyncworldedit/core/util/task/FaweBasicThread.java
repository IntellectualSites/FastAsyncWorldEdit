package com.fastasyncworldedit.core.util.task;

import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FaweBasicThread extends Thread implements FaweThread {

    private Extent currentExtent;

    protected FaweBasicThread() {
        super();
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
