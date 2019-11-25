package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class SingleBlockTypeMask extends ABlockMask {
    private final BlockType type;

    public SingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.type = type;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(vector.getBlock(getExtent()));
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getBlockType().equals(type);
    }

    @Override
    public Mask inverse() {
        return new InverseSingleBlockTypeMask(getExtent(), BlockTypesCache.values[internalId]);
    }

    public BlockType getBlockType() {
        return type;
    }
}
