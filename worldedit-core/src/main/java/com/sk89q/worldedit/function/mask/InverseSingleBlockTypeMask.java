package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class InverseSingleBlockTypeMask extends ABlockMask {
    private final int internalId;
    private final boolean replacesAir;

    public InverseSingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.internalId = type.getInternalId();
        this.replacesAir = type.getMaterial().isAir();
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

    @Override
    public Mask copy() {
        // The mask is not mutable. There is no need to clone it.
        return this;
    }

    @Override
    public boolean replacesAir() {
        return replacesAir;
    }
}
