package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class DataMask extends AbstractExtentMask implements ResettableMask {

    public DataMask(Extent extent) {
        super(extent);
    }

    private transient int data = -1;

    @Override
    public boolean test(Vector vector) {
        Extent extent = getExtent();
        if (data != -1) {
            return extent.getLazyBlock(vector).getInternalPropertiesId() == data;
        } else {
            data = extent.getLazyBlock(vector).getInternalPropertiesId();
            return true;
        }
    }

    @Override
    public void reset() {
        this.data = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
