package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.CharFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.beta.IGetBlocks;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

public abstract class CharGetBlocks extends CharBlocks implements IGetBlocks {
    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        return BlockTypes.states[get(x, y, z)].toBaseBlock();
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        return BlockTypes.states[get(x, y, z)];
    }

    @Override
    public boolean trim(final boolean aggressive) {
        for (int i = 0; i < 16; i++) {
            sections[i] = NULL;
            blocks[i] = null;
        }
        return true;
    }

    @Override
    public void reset() {
        super.reset();
    }
}