package com.boydti.fawe.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

/**
 * Represents a chunk in the queue {@link IQueueExtent}
 * Used for getting and setting blocks / biomes / entities
 */
public interface IChunk extends Trimable {
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
     * Apply the queued async changes to the world
     * @return false if applySync needs to run
     */
    boolean applyAsync();

    /**
     * Apply the queued sync changes to the world
     * @return true
     */
    boolean applySync();

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
