package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractExtentPattern extends AbstractPattern {

    private final transient Extent extent;

    public AbstractExtentPattern(Extent extent) {
        checkNotNull(extent);
        this.extent = extent;
    }

    public final Extent getExtent() {
        return extent;
    }

}
