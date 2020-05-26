package com.boydti.fawe.beta.implementation.filter.block;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlockMask;
import com.boydti.fawe.beta.Flood;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;

/**
 * Filter block implementation which uses chunks
 * - First call initChunk
 * - Then for each layer, call initLayer
 * - Then use whatever filter method you want, to iterate over the blocks in that layer
 */
public abstract class ChunkFilterBlock extends AbstractExtentFilterBlock {

    public ChunkFilterBlock(Extent extent) {
        super(extent);
    }

    /**
     * Initialize with chunk coordinates
     *  - The layer must also be initialized
     * @param chunkX
     * @param chunkZ
     * @return
     */
    public abstract ChunkFilterBlock initChunk(int chunkX, int chunkZ);

    /**
     * Initialize a chunk layer
     *  - The chunk coordinates must also be initialized first
     * @param iget
     * @param iset
     * @param layer
     * @return
     */
    public abstract ChunkFilterBlock initLayer(IBlocks iget, IChunkSet iset, int layer);

    public abstract void flood(IChunkGet iget, IChunkSet iset, int layer,
                               Flood flood, FilterBlockMask mask);


    /**
     * Filter a single block
     * @param filter
     * @param x
     * @param y
     * @param z
     */
    public abstract void filter(Filter filter, int x, int y, int z);

    /**
     * Filter a cuboid region
     * @param filter
     * @param minX
     * @param minY
     * @param minZ
     * @param maxX
     * @param maxY
     * @param maxZ
     */
    public abstract void filter(Filter filter, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ);

    /**
     * Filter everything in the layer
     * @param filter
     */
    public abstract void filter(Filter filter);

    /**
     * Filter everything between y layers
     * @param filter
     * @param yStart
     * @param yEnd
     */
    public abstract void filter(Filter filter, int yStart, int yEnd);

    /**
     * Filter with a region
     * @param filter
     * @param region
     */
    public abstract void filter(Filter filter, Region region);

    /**
     * Filter with a chunk object
     * @param chunk
     * @param get
     * @param set
     * @param filter
     * @return
     */
    public final IChunkSet filter(IChunk chunk, IChunkGet get, IChunkSet set, Filter filter) {
        initChunk(chunk.getX(), chunk.getZ());
        for (int layer = 0; layer < 16; layer++) {
            if (set.hasSection(layer)) {
                initLayer(get, set, layer);
                filter(filter);
            }
        }
        return set;
    }

    /**
     * Filter a chunk with a region / filter
     * @param chunk
     * @param get
     * @param set
     * @param filter
     * @param region
     * @param full
     * @return
     */
    public final IChunkSet filter(IChunk chunk, IChunkGet get, IChunkSet set, Filter filter, Region region, boolean full) {
        if (region != null) {
            region.filter(chunk, filter, this, get, set, full);
        } else {
            for (int layer = 0; layer < 16; layer++) {
                if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
                    continue;
                }
                initLayer(get, set, layer);
                filter(filter);
            }
        }
        return set;
    }
}
