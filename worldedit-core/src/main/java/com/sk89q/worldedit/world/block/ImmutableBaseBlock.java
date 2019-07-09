package com.sk89q.worldedit.world.block;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;

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
    public String getNbtId() {
        return "";
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBlock(extent, toBlockState());
    }

    @Override
    public <V> BaseBlock with(Property<V> property, V value) {
        return toImmutableState().with(property, value).toBaseBlock();
    }

    @Override
    public BaseBlock toBaseBlock(CompoundTag compoundTag) {
        if (compoundTag != null) {
            return new BaseBlock(this.toImmutableState(), compoundTag);
        }
        return this;
    }
}
