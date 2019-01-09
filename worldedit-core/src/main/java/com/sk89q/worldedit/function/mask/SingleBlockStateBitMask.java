package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class SingleBlockStateBitMask extends AbstractExtentMask {
    private final int bitMask;

    protected SingleBlockStateBitMask(Extent extent, int bitMask) {
        super(extent);
        this.bitMask = bitMask;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int internalId = getExtent().getBlock(vector).getInternalId();
        return (internalId & bitMask) == internalId;
    }
}
