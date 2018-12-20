package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class IdMask extends AbstractExtentMask implements ResettableMask {

    private transient int id = -1;

    public IdMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(Vector vector) {
        Extent extent = getExtent();
        if (id != -1) {
            return extent.getLazyBlock(vector).getInternalBlockTypeId() == id;
        } else {
            id = extent.getLazyBlock(vector).getInternalBlockTypeId();
            return true;
        }
    }

    @Override
    public void reset() {
        this.id = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
