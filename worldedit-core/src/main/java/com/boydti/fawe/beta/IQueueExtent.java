package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.IChunkExtent;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.implementation.processors.IBatchProcessorHolder;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.io.Flushable;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * TODO: implement Extent (need to refactor Extent first) Interface for a queue based extent which
 * uses chunks
 */
public interface IQueueExtent<T extends IChunk> extends Flushable, Trimable, IChunkExtent<T>, IBatchProcessorHolder {

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
     * Must ensure it is not in the queue handler (i.e. does not change blocks in the world)
     */
    @Override
    void disableQueue();


    /**
     * Initialize the queue (for reusability)
     * @param extent
     * @param get
     * @param set
     */
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
     * Submit the chunk so that it's changes are applied to the world
     *
     * @param chunk
     * @return result
     */
    <V extends Future<V>> V submit(T chunk);

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
     * #wrap(T)}
     *
     * @param isFull true if a more optimized chunk should be returned
     * @return a more optimized chunk object
     */
    T create(boolean isFull);

    /**
     * Wrap the chunk object (i.e. for region restrictions / limits etc.)
     *
     * @param root
     * @return wrapped chunk
     */
    default T wrap(T root) {
        return root;
    }

    @Nullable
    @Override
    default Operation commit() {
        flush();
        return null;
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

    default ChunkFilterBlock apply(ChunkFilterBlock block, Filter filter, Region region, int chunkX, int chunkZ) {
        if (!filter.appliesChunk(chunkX, chunkZ)) {
            return block;
        }
        T chunk = this.getOrCreateChunk(chunkX, chunkZ);
        // Initialize
        chunk.init(this, chunkX, chunkZ);

        T newChunk = filter.applyChunk(chunk, region);
        if (newChunk != null) {
            chunk = newChunk;
            if (block == null) {
                block = this.initFilterBlock();
            }
            chunk.filterBlocks(filter, block, region);
        }
        this.submit(chunk);
        return block;
    }

    @Override
    default <T extends Filter> T apply(Region region, T filter) {
        final Set<BlockVector2> chunks = region.getChunks();
        ChunkFilterBlock block = null;
        for (BlockVector2 chunk : chunks) {
            block = apply(block, filter, region, chunk.getX(), chunk.getZ());
        }
        flush();
        return filter;
    }
}
