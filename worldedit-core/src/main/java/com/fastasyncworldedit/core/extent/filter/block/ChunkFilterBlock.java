package com.fastasyncworldedit.core.extent.filter.block;

import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.FilterBlockMask;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.Flood;
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

    public abstract void flood(
            IChunkGet iget, IChunkSet iset, int layer,
            Flood flood, FilterBlockMask mask
    );


    /**
     * Filter a single block.
     */
    public abstract void filter(Filter filter, int x, int y, int z);

    /**
     * Filter a cuboid region.
     */
    public abstract void filter(
            Filter filter, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ
    );

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
    public synchronized final IChunkSet filter(IChunk chunk, IChunkGet get, IChunkSet set, Filter filter) {
        initChunk(chunk.getX(), chunk.getZ());
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
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
    public synchronized final IChunkSet filter(
            IChunk chunk,
            IChunkGet get,
            IChunkSet set,
            Filter filter,
            Region region,
            boolean full
    ) {
        if (region != null) {
            region.filter(chunk, filter, this, get, set, full);
        } else {
            for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
                //if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
                if (!full && !get.hasSection(layer)) {
                    continue;
                }
                initLayer(get, set, layer);
                filter(filter);
            }
        }
        return set;
    }

}
