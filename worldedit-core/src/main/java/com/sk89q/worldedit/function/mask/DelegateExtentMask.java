package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class DelegateExtentMask extends AbstractDelegateMask {
    private final Extent extent;

    public DelegateExtentMask(Extent extent, Mask parent) {
        super(parent);
        this.extent = extent;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return super.test(extent, vector);
    }

    public Extent getExtent() {
        return extent;
    }

    @Override
    public Mask withExtent(Extent extent) {
        return new DelegateExtentMask(extent, getMask());
    }
}
