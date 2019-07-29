package com.boydti.fawe.beta;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;

public abstract class ChunkFilterBlock extends SimpleFilterBlock {
    public ChunkFilterBlock(Extent extent) {
        super(extent);
    }

    public abstract ChunkFilterBlock init(int chunkX, int chunkZ, IChunkGet chunk);

    public abstract ChunkFilterBlock init(final IChunkGet iget, final IChunkSet iset, final int layer);

    public abstract void flood(final IChunkGet iget, final IChunkSet iset, final int layer, Flood flood, FilterBlockMask mask);


    public abstract void filter(Filter filter, int x, int y, int z);

    public abstract void filter(Filter filter, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    public abstract void filter(Filter filter);

    public abstract void filter(Filter filter, int yStart, int yEnd);

    public abstract void filter(final Filter filter, final Region region);
}
