package com.boydti.fawe.beta.implementation.filter.block;

import com.sk89q.worldedit.extent.Extent;

/**
 * Filter block with an extent
 */
public abstract class AbstractExtentFilterBlock extends FilterBlock {

    private final Extent extent;

    public AbstractExtentFilterBlock(Extent extent) {
        this.extent = extent;
    }

    @Override
    public final Extent getExtent() {
        return extent;
    }
}
