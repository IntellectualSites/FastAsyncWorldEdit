package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import javax.annotation.Nullable;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class HeightmapProcessor implements IBatchProcessor {
    private static final HeightMapType[] TYPES = HeightMapType.values();
    private static final BlockType RESERVED = BlockTypes.__RESERVED__;
    private static final int SECTION_SIDE_LENGTH = 16;
    private static final int BLOCKS_PER_Y_LEVEL = SECTION_SIDE_LENGTH * SECTION_SIDE_LENGTH;

    private final int maxY;
    private final int minY;

    public HeightmapProcessor(World world) {
        this.maxY = world.getMaxY();
        this.minY = world.getMinY();
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        // each heightmap gets one 16*16 array
        int[][] heightmaps = new int[TYPES.length][BLOCKS_PER_Y_LEVEL];
        BitSet[] updated = new BitSet[TYPES.length];
        for (int i = 0; i < updated.length; i++) {
            updated[i] = new BitSet(BLOCKS_PER_Y_LEVEL);
        }
        int skip = 0;
        int allSkipped = (1 << TYPES.length) - 1; // lowest types.length bits are set
        for (int y = maxY; y >= minY; y--) {
            boolean hasSectionSet = set.hasSection(y >> 4);
            boolean hasSectionGet = get.hasSection(y >> 4);
            if (!(hasSectionSet || hasSectionGet)) {
                y -= (SECTION_SIDE_LENGTH - 1); // - 1, as we do y-- in the loop head
                continue;
            }
            for (int z = 0; z < SECTION_SIDE_LENGTH; z++) {
                for (int x = 0; x < SECTION_SIDE_LENGTH; x++) {
                    BlockState block = null;
                    if (hasSectionSet) {
                        block = set.getBlock(x, y, z);
                    }
                    if (block == null || block.getBlockType() == RESERVED) {
                        if (!hasSectionGet) continue;
                        block = get.getBlock(x, y, z);
                    }
                    // fast skip if block isn't relevant for any height map
                    if (block.isAir()) continue;
                    for (int i = 0; i < TYPES.length; i++) {
                        if ((skip & (1 << i)) != 0) continue; // skip finished height map
                        HeightMapType type = TYPES[i];
                        int index = (z << 4) | x;
                        if (!updated[i].get(index) // ignore if that position was already set
                                && type.includes(block)) {
                            heightmaps[i][index] = y + 1; // mc requires + 1
                            updated[i].set(index); // mark as updated
                        }
                    }
                }
            }
            for (int i = 0; i < updated.length; i++) {
                if ((skip & (1 << i)) == 0 // if already true, skip cardinality calculation
                        && updated[i].cardinality() == BLOCKS_PER_Y_LEVEL) {
                    skip |= 1 << i;
                }
            }
            if (skip != allSkipped) continue;
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
    public @Nullable Extent construct(Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }
}
