package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

public class DataMask extends AbstractExtentMask implements ResettableMask {

    public DataMask(Extent extent) {
        super(extent);
    }

    private transient int data = -1;

    @Override
    public boolean test(BlockVector3 vector) {
        Extent extent = getExtent();
        if (data != -1) {
            return extent.getBlock(vector).getInternalPropertiesId() == data;
        } else {
            data = extent.getBlock(vector).getInternalPropertiesId();
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
