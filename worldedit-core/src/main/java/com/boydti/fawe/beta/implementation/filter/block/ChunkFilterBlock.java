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
     * Initialize with chunk coordinates. The layer must also be initialized.
     */
    public abstract ChunkFilterBlock initChunk(int chunkX, int chunkZ);

    /**
     * Initialize a chunk layer. the Chunk coordinates need to be initialized first.
     */
    public abstract ChunkFilterBlock initLayer(IBlocks iget, IChunkSet iset, int layer);

    public abstract void flood(IChunkGet iget, IChunkSet iset, int layer,
                               Flood flood, FilterBlockMask mask);


    /**
     * Filter a single block.
     */
    public abstract void filter(Filter filter, int x, int y, int z);

    /**
     * Filter a cuboid region.
     */
    public abstract void filter(Filter filter, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ);

    /**
     * Filter everything in the layer.
     */
    public abstract void filter(Filter filter);

    /**
     * Filter everything between y layers.
     */
    public abstract void filter(Filter filter, int startY, int endY);

    /**
     * Filter with a region.
     */
    public abstract void filter(Filter filter, Region region);

    /**
     * Filter with a chunk object.
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
     * Filter a chunk with a region / filter.
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
