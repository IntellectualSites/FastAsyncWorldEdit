package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.Flushable;
import java.util.concurrent.Future;

/**
 * TODO: implement Extent (need to refactor Extent first)
 * Interface for a queue based extent which uses chunks
 */
public interface IQueueExtent extends Flushable, Trimable {
    void init(WorldChunkCache world);

    /**
     * Get the {@link WorldChunkCache}
     * @return
     */
    WorldChunkCache getCache();

    /**
     * Get the IChunk at a position (and cache it if it's not already)
     * @param X
     * @param Z
     * @return IChunk
     */
    IChunk getCachedChunk(int X, int Z);

    /**
     * Submit the chunk so that it's changes are applied to the world
     * @param chunk
     * @return result
     */
    <T extends Future<T>> T submit(IChunk<T> chunk);

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
     * Create a new root IChunk object<br>
     *  - Full chunks will be reused, so a more optimized chunk can be returned in that case<br>
     *  - Don't wrap the chunk, that should be done in {@link #wrap(IChunk)}
     * @param full
     * @return
     */
    IChunk create(boolean full);

    /**
     * Wrap the chunk object (i.e. for region restrictions / limits etc.)
     * @param root
     * @return wrapped chunk
     */
    default IChunk wrap(IChunk root) {
        return root;
    }

    /**
     * Flush all changes to the world
     *  - Best to call this async so it doesn't hang the server
     */
    @Override
    void flush();

    FilterBlock initFilterBlock();
}