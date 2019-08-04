package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.io.Flushable;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * TODO: implement Extent (need to refactor Extent first) Interface for a queue based extent which
 * uses chunks
 */
public interface IQueueExtent extends Flushable, Trimable, Extent {

    @Override
    default boolean isQueueEnabled() {
        return true;
    }

    /**
     * Must ensure that it is enqueued with QueueHandler
     */
    @Override
    void enableQueue();

    /**
     * Must ensure it is not in the queue handler
     */
    @Override
    void disableQueue();

    void init(WorldChunkCache world);

    /**
     * Get the {@link WorldChunkCache}
     *
     * @return
     */
    IChunkGet getCachedGet(int x, int z, Supplier<IChunkGet> supplier);

    /**
     * Get the IChunk at a position (and cache it if it's not already)
     *
     * @param x
     * @param z
     * @return IChunk
     */
    IChunk getCachedChunk(int x, int z);

    /**
     * Submit the chunk so that it's changes are applied to the world
     *
     * @param chunk
     * @return result
     */
    <T extends Future<T>> T submit(IChunk<T> chunk);

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder state) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getFullBlock(x & 15, y, z & 15);
    }

    default BiomeType getBiome(int x, int z) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBiomeType(x & 15, z & 15);
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return BlockVector3.at(-30000000, 0, -30000000);
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return BlockVector3.at(30000000, FaweCache.WORLD_MAX_Y, 30000000);
    }

    /**
     * Create a new root IChunk object<br> - Full chunks will be reused, so a more optimized chunk
     * can be returned in that case<br> - Don't wrap the chunk, that should be done in {@link
     * #wrap(IChunk)}
     *
     * @param isFull true if a more optimized chunk should be returned
     * @return
     */
    IChunk create(boolean isFull);

    /**
     * Wrap the chunk object (i.e. for region restrictions / limits etc.)
     *
     * @param root
     * @return wrapped chunk
     */
    default IChunk wrap(IChunk root) {
        return root;
    }

    /**
     * Flush all changes to the world - Best to call this async so it doesn't hang the server
     */
    @Override
    void flush();

    ChunkFilterBlock initFilterBlock();

    int size();

    boolean isEmpty();

    void sendChunk(int chunkX, int chunkZ, int bitMask);
}
