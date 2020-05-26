package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class InverseSingleBlockTypeMask extends ABlockMask {
    private final int internalId;

    public InverseSingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.internalId = type.getInternalId();
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(vector.getBlock(extent));
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getBlockType().getInternalId() != internalId;
    }

    @Override
    public Mask inverse() {
        return new SingleBlockTypeMask(getExtent(), BlockTypesCache.values[internalId]);
    }

    public BlockType getBlockType() {
        return BlockTypes.get(internalId);
    }
}
