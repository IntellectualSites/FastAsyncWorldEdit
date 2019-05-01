package com.boydti.fawe.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Represents a chunk in the queue {@link IQueueExtent}
 * Used for getting and setting blocks / biomes / entities
 */
public interface IChunk<T extends Future<T>> extends Trimable, Callable<T> {
    /**
     * Initialize at the location
     * @param extent
     * @param X
     * @param Z
     */
    void init(IQueueExtent extent, int X, int Z);

    int getX();

    int getZ();



    /**
     * If the chunk is a delegate, returns it's paren'ts root
     * @return root IChunk
     */
    default IChunk getRoot() {
        return this;
    }

    /**
     * @return true if no changes are queued for this chunk
     */
    boolean isEmpty();

    /**
     * Spend time optimizing for apply<br>
     * default behavior: do nothing
     */
    default void optimize() {

    }

    /**
     * Apply the queued changes to the world<br>
     * The future returned may return another future<br>
     * To ensure completion keep calling {@link Future#get()} on each result
     * @return Futures
     */
    T call();

    /**
     * Call and join
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

    /* set - queues a change */
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder block);

    /**
     * Set using the filter
     * @param filter
     */
    void set(Filter filter);

    /* get - from the world */
    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    BaseBlock getFullBlock(int x, int y, int z);

    void filter(Filter filter, FilterBlock mutable);
}
