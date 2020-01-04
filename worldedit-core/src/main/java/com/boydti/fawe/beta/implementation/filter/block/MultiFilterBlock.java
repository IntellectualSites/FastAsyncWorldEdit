package com.boydti.fawe.beta.implementation.filter.block;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;

public class MultiFilterBlock extends DelegateFilterBlock {
    private final FilterBlock[] blocks;
    private final int length;

    public MultiFilterBlock(FilterBlock... blocks) {
        super(blocks[0]);
        this.blocks = blocks;
        this.length = blocks.length;
    }

    @Override
    public void setOrdinal(int ordinal) {
        for (int i = 0; i < length; i++) blocks[i].setOrdinal(ordinal);
    }

    @Override
    public void setBlock(BlockState state) {
        for (int i = 0; i < length; i++) blocks[i].setBlock(state);
    }

    @Override
    public void setFullBlock(BaseBlock block) {
        for (int i = 0; i < length; i++) blocks[i].setFullBlock(block);
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        for (int i = 0; i < length; i++) blocks[i].setNbtData(nbtData);
    }
}
