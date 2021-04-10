package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.lighting.HeightMapType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class HeightmapProcessor implements IBatchProcessor {
    private static final HeightMapType[] types = HeightMapType.values();

    private final World world;

    public HeightmapProcessor(World world) {
        this.world = world;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int max = world.getMaxY();
        int min = world.getMinY();
        int[][] heightmaps = new int[types.length][256];
        BitSet[] bitSets = new BitSet[types.length];
        for (int i = 0; i < bitSets.length; i++) {
            bitSets[i] = new BitSet(256);
        }
        boolean[] skip = new boolean[types.length];
        yLoop:
        for (int y = max; y >= min; y--) {
            if (!(set.hasSection(y >> 4) || get.hasSection(y >> 4))) {
                y -= 16;
                continue;
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState block;
                    if (set.hasSection(y >> 4)) {
                        block = set.getBlock(x, y, z);
                    } else {
                        block = get.getBlock(x, y, z);
                    }
                    for (int i = 0; i < types.length; i++) {
                        if (skip[i]) continue;
                        HeightMapType type = types[i];
                        int index = (z << 4) | x;
                        if (heightmaps[i][index] == 0 && type.blocks(block)) {
                            heightmaps[i][index] = y + 1;
                            bitSets[i].set(index);
                        }
                    }
                }
            }
            for (int i = 0; i < bitSets.length; i++) {
                if (bitSets[i].cardinality() == 256) {
                    skip[i] = true;
                }
            }
            for (boolean skipIt : skip) {
                if (!skipIt) continue yLoop;
            }
            break;
        }
        for (int i = 0; i < types.length; i++) {
            set.setHeightMap(types[i], heightmaps[i]);
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
