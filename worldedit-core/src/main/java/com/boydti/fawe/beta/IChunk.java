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
 * Represents a chunk in the queue {@link IQueueExtent} Used for getting and setting blocks / biomes
 * / entities
 */
public interface IChunk extends Trimable, IChunkGet, IChunkSet {
    /**
     * Initialize at the location
     * (allows for reuse)
     *  - It's expected initialization will clear any set fields
     * @param extent
     * @param x
     * @param z
     */
    default void init(IQueueExtent extent, int x, int z) {}
    /**
     * Get chunkX
     * @return
     */
    int getX();

    /**
     * Get chunkZ
     * @return
     */
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
     * Filter through all the blocks in the chunk
     *
     * @param filter the filter
     * @param block The filter block
     * @param region The region allowed to filter (may be null)
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
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setTile(int x, int y, int z, CompoundTag tag);

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
    default IChunk reset() {
        return this;
    }
}
