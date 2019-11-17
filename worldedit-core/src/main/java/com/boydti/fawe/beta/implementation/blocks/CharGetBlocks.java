package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public abstract class CharGetBlocks extends CharBlocks implements IChunkGet {

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)].toBaseBlock();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    @Override
    public boolean trim(boolean aggressive) {
        synchronized (this) {
            for (int i = 0; i < 16; i++) {
                sections[i] = EMPTY;
                blocks[i] = null;
            }
        }
        return true;
    }

    @Override
    public IChunkSet reset() {
        super.reset();
        return null;
    }
}
