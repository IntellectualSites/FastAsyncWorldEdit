package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;

public class BlockTypeMask extends AbstractExtentMask implements ResettableMask {

    private transient BlockType id;

    public BlockTypeMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        Extent extent = getExtent();
        if (id != null) {
            return extent.getBlock(vector).getBlockType() == id;
        } else {
            id = extent.getBlock(vector).getBlockType();
            return true;
        }
    }

    @Override
    public void reset() {
        this.id = null;
    }

}
