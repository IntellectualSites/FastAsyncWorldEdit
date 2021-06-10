package com.sk89q.worldedit.world.block;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * BaseBlock that when parsed to masks represents all BlockStates of a BlockType, whilst allowing for NBT storage
 */
public final class BlanketBaseBlock extends BaseBlock {

    public BlanketBaseBlock(BlockState blockState) {
        super(blockState);
    }

    public BlanketBaseBlock(BlockState blockState, @NotNull CompoundTag tile) {
        super(blockState, tile);
    }

    @Override
    public BaseBlock toBaseBlock(CompoundTag compoundTag) {
        if (compoundTag != null) {
            return new BaseBlock(this.toImmutableState(), compoundTag);
        }
        return this;
    }
}
