package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.IBatchProcessorHolder;
import com.fastasyncworldedit.core.internal.simd.SimdSupport;
import com.fastasyncworldedit.core.internal.simd.VectorizedCharFilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import com.fastasyncworldedit.core.queue.implementation.chunk.WrapperChunk;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.SideEffectSet;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.io.Flushable;
import java.util.Set;
import java.util.concurrent.Callable;
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
    IChunkGet getCachedGet(int chunkX, int chunkZ);

    /**
     * Get the cached chunk set object.
     */
    IChunkSet getCachedSet(int chunkX, int chunkZ);

    /**
     * Submit the chunk so that its changes are applied to the world
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
     * Set the side effects to be used with this extent
     *
     * @since 2.12.3
     */
    void setSideEffectSet(SideEffectSet sideEffectSet);

    /**
     * Get the side effects to be used with this extent
     *
     * @since 2.12.3
     */
    SideEffectSet getSideEffectSet();

    /**
     * Submit a task to the extent to be queued as if it were a chunk
     */
    @ApiStatus.Internal
    <V extends Future<V>> V submitTaskUnchecked(Callable<V> callable);

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
    ChunkFilterBlock createFilterBlock();

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

    default ChunkFilterBlock apply(
            @Nullable ChunkFilterBlock block,
            Filter filter,
            Region region,
            int chunkX,
            int chunkZ,
            boolean full
    ) {
//        if (!filter.appliesChunk(chunkX, chunkZ)) {
//            return block;
//        }
        T initial = this.getOrCreateChunk(chunkX, chunkZ);
        WrapperChunk<T> chunk = new WrapperChunk<>(initial, () -> this.getOrCreateChunk(chunkX, chunkZ));

        IChunk newChunk = filter.applyChunk(chunk, region);
        if (newChunk == chunk) {
            newChunk = chunk.get();
        } else {
            chunk.setWrapped((T) newChunk);
        }
        if (newChunk != null) {
            if (block == null) {
                if (SimdSupport.useVectorApi() && filter instanceof VectorizedFilter) {
                    block = new VectorizedCharFilterBlock(this);
                } else {
                    block = this.createFilterBlock();
                }
            }
            block.initChunk(chunkX, chunkZ);
            chunk.filterBlocks(filter, block, region, full);
        }
        // If null, then assume it has already been submitted and the WrapperChunk has therefore been invalidated
        T toSubmit = chunk.get();
        if (toSubmit != null) {
            this.submit(toSubmit);
        }
        return block;
    }

    @Override
    default <U extends Filter> U apply(Region region, U filter, boolean full) {
        final Set<BlockVector2> chunks = region.getChunks();
        ChunkFilterBlock block = null;
        for (BlockVector2 chunk : chunks) {
            block = apply(block, filter, region, chunk.x(), chunk.z(), full);
        }
        flush();
        return filter;
    }

}
