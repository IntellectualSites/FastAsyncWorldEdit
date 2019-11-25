package com.boydti.fawe.object.pattern;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

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
