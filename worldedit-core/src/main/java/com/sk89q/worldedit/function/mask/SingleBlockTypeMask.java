package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class SingleBlockTypeMask extends ABlockMask {
    private final int internalId;
    private final boolean isAir;

    public SingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        isAir = type == BlockTypes.AIR || type == BlockTypes.CAVE_AIR || type == BlockTypes.VOID_AIR;
            this.internalId = type.getInternalId();
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getBlockType().getInternalId() == internalId;
    }

    @Override
    public Mask inverse() {
        return new InverseSingleBlockTypeMask(getExtent(), BlockTypesCache.values[internalId]);
    }

    public BlockType getBlockType() {
        return BlockTypes.get(internalId);
    }

    @Override
    public boolean replacesAir() {
        return isAir;
    }
}
