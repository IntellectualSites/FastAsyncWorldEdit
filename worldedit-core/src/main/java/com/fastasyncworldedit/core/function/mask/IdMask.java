package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.concurrent.atomic.AtomicInteger;

public class IdMask extends AbstractExtentMask implements ResettableMask {

    private final AtomicInteger id = new AtomicInteger(-1);

    public IdMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        int blockID = extent.getBlock(vector).getInternalBlockTypeId();
        int testId = id.compareAndExchange(-1, blockID);
        return blockID == testId || testId == -1;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int blockID = vector.getBlock(getExtent()).getInternalBlockTypeId();
        int testId = id.compareAndExchange(-1, blockID);
        return blockID == testId || testId == -1;
    }

    @Override
    public void reset() {
        this.id.set(-1);
    }

    @Override
    public Mask copy() {
        return this;
    }

    @Override
    public boolean replacesAir() {
        return true;
    }

}
