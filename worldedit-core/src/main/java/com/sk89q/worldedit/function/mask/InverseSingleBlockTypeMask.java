package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

public class InverseSingleBlockTypeMask extends ABlockMask {
    private final BlockType type;

    public InverseSingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.type = type;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(getExtent().getBlock(vector));
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getBlockType().equals(type);
    }

    @Override
    public Mask inverse() {
        return new SingleBlockTypeMask(getExtent(), type);
    }

    public BlockType getBlockType() {
        return this.type;
    }
}
