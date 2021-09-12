package com.fastasyncworldedit.core.extent.processor.heightmap;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class HeightmapProcessor implements IBatchProcessor {

    private static final HeightMapType[] TYPES = HeightMapType.values();
    private static final int BLOCKS_PER_Y_SHIFT = 8; // log2(256)
    private static final int BLOCKS_PER_Y = 256; // 16 x 16
    private static final boolean[] COMPLETE = new boolean[BLOCKS_PER_Y];
    private static final char[] AIR_LAYER = new char[4096];

    static {
        Arrays.fill(COMPLETE, true);
        Arrays.fill(AIR_LAYER, (char) 1);
    }

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
        boolean[][] updated = new boolean[TYPES.length][BLOCKS_PER_Y];
        int skip = 0;
        int allSkipped = (1 << TYPES.length) - 1; // lowest types.length bits are set
        layer:
        for (int layer = maxY >> 4; layer >= minY >> 4; layer--) {
            boolean hasSectionSet = set.hasSection(layer);
            boolean hasSectionGet = get.hasSection(layer);
            if (!(hasSectionSet || hasSectionGet)) {
                continue;
            }
            char[] setSection = hasSectionSet ? set.loadIfPresent(layer) : null;
            if (setSection == null || Arrays.equals(setSection, FaweCache.IMP.EMPTY_CHAR_4096) ||
                    Arrays.equals(setSection, AIR_LAYER)) {
                hasSectionSet = false;
            }
            if (!hasSectionSet && !hasSectionGet) {
                continue;
            }
            char[] getSection = null;
            for (int y = 15; y >= 0; y--) {
                // We don't need to actually iterate over x and z as we're both reading and writing an index
                for (int j = 0; j < BLOCKS_PER_Y; j++) {
                    char ordinal = 0;
                    if (hasSectionSet) {
                        ordinal = setSection[index(y, j)];
                    }
                    if (ordinal == 0) {
                        if (!hasSectionGet) {
                            if (!hasSectionSet) {
                                continue layer;
                            }
                            continue;
                        } else if (getSection == null) {
                            getSection = get.load(layer);
                            // skip empty layer
                            if (Arrays.equals(getSection, FaweCache.IMP.EMPTY_CHAR_4096)
                                    || Arrays.equals(getSection, AIR_LAYER)) {
                                hasSectionGet = false;
                                if (!hasSectionSet) {
                                    continue layer;
                                }
                                continue;
                            }
                        }
                        ordinal = getSection[index(y, j)];
                    }
                    // fast skip if block isn't relevant for any height map (air or empty)
                    if (ordinal < 4) {
                        continue;
                    }
                    BlockState block = BlockTypesCache.states[ordinal];
                    if (block == null) {
                        continue;
                    }
                    for (int i = 0; i < TYPES.length; i++) {
                        if ((skip & (1 << i)) != 0) {
                            continue; // skip finished height map
                        }
                        HeightMapType type = TYPES[i];
                        // ignore if that position was already set
                        if (!updated[i][j] && type.includes(block)) {
                            // mc requires + 1, heightmaps are normalized internally, thus we need to "zero" them.
                            heightmaps[i][j] = ((layer - get.getMinSectionPosition()) << 4) + y + 1;
                            updated[i][j] = true; // mark as updated
                        }
                    }
                }
            }
            for (int i = 0; i < updated.length; i++) {
                if ((skip & (1 << i)) == 0 // if already true, skip array equality check
                        && Arrays.equals(updated[i], COMPLETE)) {
                    skip |= 1 << i;
                }
            }
            if (skip != allSkipped) {
                continue;
            }
            break; // all maps are processed
        }
        for (int i = 0; i < TYPES.length; i++) {
            set.setHeightMap(TYPES[i], heightmaps[i]);
        }
        return set;
    }

    @Override
    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return CompletableFuture.completedFuture(set);
    }

    @Override
    @Nullable
    public Extent construct(Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }

}
