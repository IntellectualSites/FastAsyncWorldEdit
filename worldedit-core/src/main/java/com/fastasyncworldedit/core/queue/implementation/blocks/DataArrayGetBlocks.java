package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public abstract class DataArrayGetBlocks extends DataArrayBlocks implements IChunkGet {

    /**
     * New instance given the min/max section indices
     */
    public DataArrayGetBlocks(final int minSectionPosition, final int maxSectionPosition) {
        super(minSectionPosition, maxSectionPosition);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = BlockTypesCache.states[get(x, y, z)];
        return state.toBaseBlock((IBlocks) this, x, y, z);
    }

    @Override
    public boolean trim(boolean aggressive) {
        for (int i = 0; i < sectionCount; i++) {
            synchronized (sectionLocks[i]) {
                blocks[i] = null;
            }
        }
        return true;
    }

    @Override
    public DataArray update(int layer, DataArray data, boolean aggressive) {
        if (data == null) {
            data = DataArray.createEmpty();
        }
        data.setAll(BlockTypesCache.ReservedIDs.AIR);
        return data;
    }

    @Override
    protected char defaultOrdinal() {
        return BlockTypesCache.ReservedIDs.AIR;
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        layer -= minSectionPosition;
        synchronized (sectionLocks[layer]) {
            blocks[layer] = null;
            return true;
        }
    }

    @Override
    public IChunkSet reset() {
        super.reset();
        return null;
    }
}
