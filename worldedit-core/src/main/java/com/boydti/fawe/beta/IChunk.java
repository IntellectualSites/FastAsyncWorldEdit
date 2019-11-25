package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import javax.annotation.Nullable;

/**
 * Represents a chunk in the queue {@link IQueueExtent}
 * Used for getting and setting blocks / biomes / entities
 */
public interface IChunk extends Trimable, IChunkGet, IChunkSet {
    /**
     * Initialize at the location
     * @param extent
     * @param X
     * @param Z
     */
    default void init(IQueueExtent extent, int x, int z) {}
    /**
     * Get chunkX
     * @return
     */
    int getX();

    int getZ();

    /**
     * If the chunk is a delegate, returns its parent's root
     *
     * @return root IChunk
     */
    default IChunk getRoot() {
        return this;
    }

    /**
     * @return true if no changes are queued for this chunk
     */
    @Override
    boolean isEmpty();

    /**
     * Filter
     * @param filter the filter
     * @param block The filter block
     * @param region The region allowed to filter (maybe null)
     */
    void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, boolean full);

//    /**
//     * Flood through all the blocks in the chunk
//     * TODO not implemented
//     * @param flood
//     * @param mask
//     * @param block
//     */
//    void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block);

    /* set - queues a change */
    @Override
    boolean setBiome(int x, int y, int z, BiomeType biome);

    @Override
    boolean setTile(int x, int y, int z, CompoundTag tag);

    @Override
    boolean setBlock(int x, int y, int z, BlockStateHolder block);

    /* get - from the world */
    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    BaseBlock getFullBlock(int x, int y, int z);

    @Override
    CompoundTag getTag(int x, int y, int z);

    @Override
    default IChunk reset() {
        return this;
    }
}
