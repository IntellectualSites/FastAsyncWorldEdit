package com.sk89q.worldedit.function.mask;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.extent.Extent;

public abstract class AbstractExtentMask extends AbstractMask {
    private transient Extent extent;

    protected AbstractExtentMask(Extent extent) {
        this.setExtent(extent);
    }

    public Extent getExtent() {
        return this.extent;
    }

    public void setExtent(Extent extent) {
        Preconditions.checkNotNull(extent);
        this.extent = extent;
    }

    public static Class<?> inject() {
        return AbstractExtentMask.class;
    }
}
