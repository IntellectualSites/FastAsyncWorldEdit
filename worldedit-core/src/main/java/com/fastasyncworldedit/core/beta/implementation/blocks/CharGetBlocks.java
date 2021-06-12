package com.fastasyncworldedit.core.beta.implementation.blocks;

import com.fastasyncworldedit.core.beta.IChunkGet;
import com.fastasyncworldedit.core.beta.IChunkSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.Arrays;

public abstract class CharGetBlocks extends CharBlocks implements IChunkGet {

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = BlockTypesCache.states[get(x, y, z)];
        return state.toBaseBlock(this, x, y, z);
    }

    @Override
    public boolean trim(boolean aggressive) {
        for (int i = 0; i < 16; i++) {
            sections[i] = empty;
            blocks[i] = null;
        }
        return true;
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            data = new char[4096];
        }
        Arrays.fill(data, (char) 1);
        return data;
    }

    @Override
    public synchronized boolean trim(boolean aggressive, int layer) {
        sections[layer] = empty;
        blocks[layer] = null;
        return true;
    }

    @Override
    public IChunkSet reset() {
        super.reset();
        return null;
    }
}
