package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class IdDataMask extends AbstractExtentMask implements ResettableMask {
    private transient int combined = -1;

    public IdDataMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        if (combined != -1) {
            return getExtent().getBlock(vector).getInternalId() == combined;
        } else {
            combined = getExtent().getBlock(vector).getInternalId();
            return true;
        }
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(getExtent(), vector);
    }

    @Override
    public void reset() {
        this.combined = -1;
    }

    @Override
    public Mask copy() {
        return new IdDataMask(getExtent());
    }

    @Override
    public boolean replacesAir() {
        return true;
    }

}
