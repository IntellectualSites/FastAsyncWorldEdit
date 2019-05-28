package com.sk89q.worldedit.world.block;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

public final class ImmutableBaseBlock extends BaseBlock {
    public ImmutableBaseBlock(BlockState blockState) {
        super(blockState);
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        return null;
    }

    @Override
    public boolean hasNbtData() {
        return false;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBlock(extent, toBlockState());
    }
}
