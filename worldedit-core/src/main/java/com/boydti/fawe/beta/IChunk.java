package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * Represents a chunk in the queue {@link IQueueExtent} Used for getting and setting blocks / biomes
 * / entities
 */
public interface IChunk<T extends Future<T>> extends Trimable, Callable<T>, IChunkGet {

    /**
     * Initialize at the location
     *
     * @param extent
     * @param x
     * @param z
     */
    void init(IQueueExtent extent, int x, int z);

    IQueueExtent getQueue();

    int getX();

    int getZ();

    /**
     * If the chunk is a delegate, returns it's parent's root
     *
     * @return root IChunk
     */
    default IChunk getRoot() {
        return this;
    }

    /**
     * Checks if there are any queued changes for this chunk.
     *
     * @return true if no changes are queued for this chunk
     */
    boolean isEmpty();

    /**
     * Apply the queued changes to the world containing this chunk.
     * <p>The future returned may return another future. To ensure completion keep calling {@link
     * Future#get()} on each result.</p>
     *
     * @return Futures
     */
    @Override
    T call();

    /**
     * Call and join
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    default void join() throws ExecutionException, InterruptedException {
        T future = call();
        while (future != null) {
            future = future.get();
        }
        return;
    }

    /**
     * Filter
     *
     * @param filter the filter
     * @param block  The filter block
     * @param region The region allowed to filter (may be null)
     */
    void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region);

    void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block);

    /* set - queues a change */
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder block);

    @Override
    BiomeType getBiomeType(int x, int z);

    @Override
    BlockState getBlock(int x, int y, int z);

    @Override
    BaseBlock getFullBlock(int x, int y, int z);

    @Override
    CompoundTag getTag(int x, int y, int z);

    @Override
    default IBlocks reset() {
        init(getQueue(), getX(), getZ());
        return this;
    }
}
