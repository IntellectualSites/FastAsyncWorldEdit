package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Range;

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
    default <V extends IChunk> void init(IQueueExtent<V> extent, int x, int z) {}

    /**
     * Get chunkX
     * @return the x coordinate of the chunk
     */
    @Range(from = 0, to = 15)
    int getX();

    /**
     * Get chunkZ
     * @return the z coordinate of the chunk
     */
    @Range(from = 0, to = 15)
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
     * Filter through all the blocks in the chunk
     *
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

    @Override
    CompoundTag getTile(int x, int y, int z);

    @Override
    default IChunk reset() {
        return this;
    }
}
