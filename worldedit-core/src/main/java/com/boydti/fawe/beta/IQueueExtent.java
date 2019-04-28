package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.Flushable;
import java.util.concurrent.Future;

public interface IQueueExtent extends Flushable, Trimable {
    void init(WorldChunkCache world);

    IChunk getCachedChunk(int X, int Z);

    <T> Future<T> submit(IChunk<T, ?> chunk);

    default boolean setBlock(final int x, final int y, final int z, final BlockStateHolder state) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    default boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
    }

    default BlockState getBlock(final int x, final int y, final int z) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    default BiomeType getBiome(final int x, final int z) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBiome(x & 15, z & 15);
    }

    /**
     * Return the IChunk
     * @param full
     * @return
     */
    IChunk create(boolean full);

    /**
     * Wrap the chunk object (i.e. for region restrictions etc.)
     * @param root
     * @return wrapped chunk
     */
    default IChunk wrap(IChunk root) {
        return root;
    }

    @Override
    void flush();
}