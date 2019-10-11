package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.IChunkCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.io.Flushable;
import java.util.concurrent.Future;

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
     * Clear any block updates
     * @param players
     */
    default void clearBlockUpdates(Player... players) {
        throw new UnsupportedOperationException("TODO NOT IMPLEMENTED");
    }

    /**
     * Send all the chunks as block updates
     * @param players
     */
    default void sendBlockUpdates(Player... players) {
        throw new UnsupportedOperationException("TODO NOT IMPLEMENTED");
    }

    /**
     * Must ensure that it is enqueued with QueueHandler
     */
    @Override
    void enableQueue();

    /**
     * Must ensure it is not in the queue handler (i.e. does not change blocks in the world)
     */
    @Override
    void disableQueue();


    void init(Extent extent, IChunkCache<IChunkGet> get, IChunkCache<IChunkSet> set);

    /**
     * Get the cached get object
     *  - Faster than getting it using NMS and allows for wrapping
     * @param x
     * @param z
     * @param supplier
     * @return
     */
    IChunkGet getCachedGet(int x, int z);

    /**
     * Get the cached chunk set object
     * @param x
     * @param z
     * @param supplier
     * @return
     */
    IChunkSet getCachedSet(int x, int z);

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

    // standard get / set

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder state) {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        final IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setTile(x & 15, y, z & 15, tile);
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
        return BlockVector3.at(30000000, FaweCache.IMP.WORLD_MAX_Y, 30000000);
    }

    /**
     * Create a new root IChunk object<br> - Full chunks will be reused, so a more optimized chunk
     * can be returned in that case<br> - Don't wrap the chunk, that should be done in {@link
     * #wrap(IChunk)}
     *
     * @param isFull true if a more optimized chunk should be returned
     * @return a more optimized chunk object
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

    /**
     * A filter block is used to iterate over blocks / positions
     *  - Essentially combines BlockVector3, Extent and BlockState functions in a way that avoids lookups
     * @return
     */
    ChunkFilterBlock initFilterBlock();

    /**
     * Returns the number of chunks in this queue.
     *
     * @return the number of chunks in this queue
     */
    int size();

    /**
     * Returns <tt>true</tt> if this queue contains no elements.
     *
     * @return <tt>true</tt> if this queue contains no elements
     */
    boolean isEmpty();
}
