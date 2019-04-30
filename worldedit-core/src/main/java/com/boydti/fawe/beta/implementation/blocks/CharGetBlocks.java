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
    public void filter(Filter filter, FilterBlock block) {
        CharFilterBlock b = (CharFilterBlock) block;
        for (int layer = 0; layer < 16; layer++) {
            if (!hasSection(layer)) continue;
            char[] arr = sections[layer].get(this, layer);
            b.init(arr, layer);
            for (b.y = 0, b.index = 0; b.y < 16; b.y++) {
                for (b.z = 0; b.z < 16; b.z++) {
                    for (b.x = 0; b.x < 16; b.x++, b.index++) {
                        filter.applyBlock(b);
                    }
                }
            }
        }
    }

    @Override
    public boolean trim(boolean aggressive) {
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