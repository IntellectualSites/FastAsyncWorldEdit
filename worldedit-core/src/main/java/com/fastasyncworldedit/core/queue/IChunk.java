package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;

/**
 * Represents a chunk in the queue {@link IQueueExtent} Used for getting and setting blocks / biomes
 * / entities
 */
public interface IChunk extends Trimable, IChunkGet, IChunkSet {

    /**
     * Initialize at the location
     * (allows for reuse)
     * - It's expected initialization will clear any set fields
     */
    default <V extends IChunk> void init(IQueueExtent<V> extent, int x, int z) {
    }

    /**
     * Get chunkX
     *
     * @return the x coordinate of the chunk
     */
    int getX();

    /**
     * Get chunkZ
     *
     * @return the z coordinate of the chunk
     */
    int getZ();

    /**
     * Return the minimum block coordinate of the chunk
     *
     * @return BlockVector3 of minimum block coordinate
     * @since TODO
     */
    default BlockVector3 getChunkBlockCoord() {
        return BlockVector3.at(getX() << 4, getMinY(), getZ() << 4);
    }

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
     * @param block  The filter block
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
    default IChunk reset() {
        return this;
    }

}
