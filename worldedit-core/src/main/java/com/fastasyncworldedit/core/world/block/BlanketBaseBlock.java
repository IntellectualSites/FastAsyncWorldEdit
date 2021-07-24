package com.fastasyncworldedit.core.world.block;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.jetbrains.annotations.NotNull;

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
