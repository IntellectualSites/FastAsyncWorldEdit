package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class BlockTypeMask extends AbstractExtentMask {
    private final boolean[] types;

    protected BlockTypeMask(Extent extent, boolean[] types) {
        super(extent);
        this.types = types;
    }

    public BlockTypeMask(Extent extent, BlockType... types) {
        super(extent);
        this.types = new boolean[BlockTypes.size()];
        for (BlockType type : types) this.types[type.getInternalId()] = true;
    }

    @Override
    public boolean test(Vector vector) {
        return types[getExtent().getBlockType(vector).getInternalId()];
    }
}
