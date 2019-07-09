package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.math.BlockVector3;

public class IdDataMask extends AbstractExtentMask implements ResettableMask {
    private transient int combined = -1;

    public IdDataMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        Extent extent = getExtent();
        if (combined != -1) {
            return extent.getLazyBlock(vector).getInternalId() == combined;
        } else {
            combined = extent.getLazyBlock(vector).getInternalId();
            return true;
        }
    }

    @Override
    public void reset() {
        this.combined = -1;
    }

}
