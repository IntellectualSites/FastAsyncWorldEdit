package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.lang.invoke.VarHandle;
import java.util.Arrays;

public abstract class CharGetBlocks extends CharBlocks implements IChunkGet {

    /**
     * New instance given the min/max section indices
     */
    public CharGetBlocks(final int minSectionPosition, final int maxSectionPosition) {
        super(minSectionPosition, maxSectionPosition);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = BlockTypesCache.states[get(x, y, z)];
        return state.toBaseBlock((IBlocks) this, x, y, z);
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = EMPTY;
            VarHandle.storeStoreFence();
            blocks[i] = null;
        }
        return true;
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            data = new char[4096];
        }
        Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
        return data;
    }

    @Override
    protected char defaultOrdinal() {
        return BlockTypesCache.ReservedIDs.AIR;
    }

    @Override
    public synchronized boolean trim(boolean aggressive, int layer) {
        layer -= minSectionPosition;
        sections[layer] = EMPTY;
        VarHandle.storeStoreFence();
        blocks[layer] = null;
        return true;
    }

    @Override
    public IChunkSet reset() {
        super.reset();
        return null;
    }

}
