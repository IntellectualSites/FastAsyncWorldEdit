package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;

public class SingleBlockStateBitMask extends AbstractExtentMask {
    private final int bitMask;

    protected SingleBlockStateBitMask(Extent extent, int bitMask) {
        super(extent);
        this.bitMask = bitMask;
    }

    @Override
    public boolean test(Vector vector) {
        int internalId = getExtent().getBlock(vector).getInternalId();
        return (internalId & bitMask) == internalId;
    }
}
