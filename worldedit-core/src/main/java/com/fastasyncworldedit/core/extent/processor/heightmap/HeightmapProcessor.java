package com.fastasyncworldedit.core.extent.processor.heightmap;

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
import java.util.Arrays;

public class HeightmapProcessor implements IBatchProcessor {

    private static final HeightMapType[] TYPES = HeightMapType.values();
    private static final int BLOCKS_PER_Y_SHIFT = 8; // log2(256)
    private static final int BLOCKS_PER_Y = 256; // 16 x 16
    private static final DataArray AIR_LAYER = DataArray.createFilled(BlockTypesCache.ReservedIDs.AIR);
    private static final int NEEDED_UPDATES = TYPES.length * BLOCKS_PER_Y;

    private final int minY;
    private final int maxY;

    /**
     * New HeightmapProcessor instance that will create heightmaps between the given heights (inclusive). If no applicable
     * block is found before the minimum Y, 0 is used.
     *
     * @param minY minimum Y to consider
     * @param maxY maximum Y to consider
     */
    public HeightmapProcessor(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    private static int index(int y, int offset) {
        return (y << BLOCKS_PER_Y_SHIFT) + offset;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        // each heightmap gets one 16*16 array
        int[][] heightmaps = new int[TYPES.length][BLOCKS_PER_Y];
        byte[] updated = new byte[BLOCKS_PER_Y];
        int updateCount = 0; // count updates, this way we know when we're finished
        layerIter:
        for (int layer = maxY >> 4; layer >= minY >> 4; layer--) {
            boolean hasSectionSet = set.hasNonEmptySection(layer);
            boolean hasSectionGet = get.hasNonEmptySection(layer);
            if (!(hasSectionSet || hasSectionGet)) {
                continue;
            }
            DataArray setSection = hasSectionSet ? set.loadIfPresent(layer) : null;
            if (setSection == null || setSection.isEmpty() ||
                    setSection.equals(AIR_LAYER)) {
                hasSectionSet = false;
            }
            if (!hasSectionSet && !hasSectionGet) {
                continue;
            }
            DataArray getSection = null;
            for (int y = 15; y >= 0; y--) {
                // We don't need to actually iterate over x and z as we're both reading and writing an index
                for (int j = 0; j < BLOCKS_PER_Y; j++) {
                    int ordinal = BlockTypesCache.ReservedIDs.__RESERVED__;
                    if (hasSectionSet) {
                        ordinal = setSection.getAt(index(y, j));
                    }
                    if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                        if (!hasSectionGet) {
                            if (!hasSectionSet) {
                                continue layerIter;
                            }
                            continue;
                        } else if (getSection == null) {
                            getSection = get.load(layer);
                            // skip empty layer
                            if (getSection.isEmpty()
                                    || getSection.equals(AIR_LAYER)) {
                                hasSectionGet = false;
                                if (!hasSectionSet) {
                                    continue layerIter;
                                }
                                continue;
                            }
                        }
                        ordinal = getSection.getAt(index(y, j));
                    }
                    // fast skip if block isn't relevant for any height map (air or empty)
                    if (ordinal < 4) {
                        continue;
                    }
                    BlockState block = BlockTypesCache.states[ordinal];
                    if (block == null) {
                        continue;
                    }
                    byte updateStateAtJ = updated[j];
                    for (int i = 0; i < TYPES.length; i++) {
                        int bitFlag = 1 << i;
                        if ((updateStateAtJ & bitFlag) != 0) {
                            continue; // skip finished height map at this column
                        }
                        HeightMapType type = TYPES[i];
                        // ignore if that position was already set
                        if (type.includes(block)) {
                            // mc requires + 1, heightmaps are normalized internally, thus we need to "zero" them.
                            heightmaps[i][j] = ((layer - get.getMinSectionPosition()) << 4) + y + 1;
                            updated[j] |= (byte) bitFlag; // mark as updated
                            if (++updateCount == NEEDED_UPDATES) {
                                break layerIter; // all heightmaps in all columns updated
                            }

                        }
                    }
                }
            }
        }
        for (int i = 0; i < TYPES.length; i++) {
            set.setHeightMap(TYPES[i], heightmaps[i]);
        }
        return set;
    }

    private void skipOrUpdateHeightmap(
            final IChunkGet get,
            final int[][] heightmaps,
            final boolean[][] updated,
            final int skip,
            final int layer,
            final int y,
            final int j,
            final BlockState state,
            final int ordinal
    ) {
        // fast skip if block isn't relevant for any height map (air or empty)
        if (ordinal < 4) {
            return;
        }
        if (state == null) {
            return;
        }
        for (int i = 0; i < TYPES.length; i++) {
            if ((skip & (1 << i)) != 0) {
                continue; // skip finished height map
            }
            HeightMapType type = TYPES[i];
            // ignore if that position was already set
            if (!updated[i][j] && type.includes(state)) {
                // mc requires + 1, heightmaps are normalized internally, thus we need to "zero" them.
                heightmaps[i][j] = ((layer - get.getMinSectionPosition()) << 4) + y + 1;
                updated[i][j] = true; // mark as updated
            }
        }
    }

    @Override
    @Nullable
    public Extent construct(Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_BLOCKS;
    }

}
