package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SingleBlockTypeMask extends ABlockMask {
    private final int internalId;

    public SingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.internalId = type.getInternalId();
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(vector.getBlock(getExtent()));
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getBlockType().getInternalId() == internalId;
    }

    @Override
    public Mask inverse() {
        return new InverseSingleBlockTypeMask(getExtent(), BlockTypes.values[internalId]);
    }

    public BlockType getBlockType() {
        return BlockTypes.get(internalId);
    }
}
