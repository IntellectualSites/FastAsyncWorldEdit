package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.IBatchProcessorHolder;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.jetbrains.annotations.Range;

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
     *
     * @param extent extent to use
     * @param get    cache of chunk GET
     * @param set    cache of chunk SET
     */
    void init(Extent extent, IChunkCache<IChunkGet> get, IChunkCache<IChunkSet> set);

    /**
     * Get the cached get object. This is faster than getting the object using NMS and allows for
     * wrapping.
     */
    IChunkGet getCachedGet(@Range(from = 0, to = 15) int chunkX, @Range(from = 0, to = 15) int chunkZ);

    /**
     * Get the cached chunk set object.
     */
    IChunkSet getCachedSet(@Range(from = 0, to = 15) int chunkX, @Range(from = 0, to = 15) int chunkZ);

    /**
     * Submit the chunk so that it's changes are applied to the world
     *
     * @return Future
     */
    <V extends Future<V>> V submit(T chunk);

    @Override
    default BlockVector3 getMinimumPoint() {
        return BlockVector3.at(-30000000, getMinY(), -30000000);
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return BlockVector3.at(30000000, getMaxY(), 30000000);
    }

    void setFastMode(boolean fastMode);

    boolean isFastMode();

    /**
     * Create a new root IChunk object. Full chunks will be reused, so a more optimized chunk can be
     * returned in that case.
     *
     * @param isFull true if a more optimized chunk should be returned
     * @return a more optimized chunk object
     * @see IQueueExtent#wrap(IChunk) Don't wrap the chunk, that should be done in
     */
    T create(boolean isFull);

    /**
     * Wrap the chunk object (i.e., for region restrictions / limits etc.).
     *
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
     * Flush all changes to the world.
     * Best to call this async, so it doesn't hang the server.
     */
    @Override
    void flush();

    /**
     * A filter block is used to iterate over blocks / positions. Essentially combines BlockVector3,
     * Extent and BlockState functions in a way that avoids lookups.
     */
    ChunkFilterBlock initFilterBlock();

    /**
     * Returns the number of chunks in this queue.
     *
     * @return the number of chunks in this queue
     */
    int size();

    /**
     * @return {@code true} if this queue contains no elements
     */
    boolean isEmpty();

    default ChunkFilterBlock apply(ChunkFilterBlock block, Filter filter, Region region, int chunkX, int chunkZ, boolean full) {
        if (!filter.appliesChunk(chunkX, chunkZ)) {
            return block;
        }
        T chunk = this.getOrCreateChunk(chunkX, chunkZ);

        T newChunk = filter.applyChunk(chunk, region);
        if (newChunk != null) {
            chunk = newChunk;
            if (block == null) {
                block = this.initFilterBlock();
            }
            chunk.filterBlocks(filter, block, region, full);
        }
        this.submit(chunk);
        return block;
    }

    @Override
    default <T extends Filter> T apply(Region region, T filter, boolean full) {
        final Set<BlockVector2> chunks = region.getChunks();
        ChunkFilterBlock block = null;
        for (BlockVector2 chunk : chunks) {
            block = apply(block, filter, region, chunk.getX(), chunk.getZ(), full);
        }
        flush();
        return filter;
    }

}
