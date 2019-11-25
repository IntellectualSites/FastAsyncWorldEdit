package com.boydti.fawe.beta;

import com.sk89q.worldedit.extent.Extent;

public abstract class SimpleFilterBlock extends FilterBlock {

    private final Extent extent;

    public SimpleFilterBlock(Extent extent) {
        this.extent = extent;
    }

    @Override
    public final Extent getExtent() {
        return extent;
    }
}
