package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;

/**
 * Common code for post-processing on different versions
 */
public abstract class PostProcessor implements IBatchProcessor {

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    protected boolean wasAdjacentToWater(DataArray get, DataArray set, int i, int x, int y, int z) {
        if (set == null || get == null) {
            return false;
        }
        int ordinal;
        int reserved = BlockTypesCache.ReservedIDs.__RESERVED__;
        if (x > 0 && set.getAt(i - 1) != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get.getAt(i - 1))] && isFluid(ordinal)) {
                return true;
            }
        }
        if (x < 15 && set.getAt(i + 1) != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get.getAt(i + 1))] && isFluid(ordinal)) {
                return true;
            }
        }
        if (z > 0 && set.getAt(i - 16) != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get.getAt(i - 16))] && isFluid(ordinal)) {
                return true;
            }
        }
        if (z < 15 && set.getAt(i + 16) != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get.getAt(i + 16))] && isFluid(ordinal)) {
                return true;
            }
        }
        if (y > 0 && set.getAt(i - 256) != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get.getAt(i - 256))] && isFluid(ordinal)) {
                return true;
            }
        }
        if (y < 15 && set.getAt(i + 256) != reserved) {
            return BlockTypesCache.ticking[(ordinal = get.getAt(i + 256))] && isFluid(ordinal);
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isFluid(int ordinal) {
        return BlockState.getFromOrdinal(ordinal).getMaterial().isLiquid();
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_BLOCKS;
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return set;
    }

}
