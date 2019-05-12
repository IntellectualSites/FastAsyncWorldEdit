package com.sk89q.worldedit.world.block;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.jnbt.CompoundTag;

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
    public final void apply(FilterBlock block) {
        block.setOrdinal(getOrdinal());
    }
}
