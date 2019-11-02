package com.boydti.fawe.beta.implementation.filter.block;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlockMask;
import com.boydti.fawe.beta.Flood;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;

public abstract class ChunkFilterBlock extends SimpleFilterBlock {

    public ChunkFilterBlock(Extent extent) {
        super(extent);
    }

    public abstract ChunkFilterBlock init(int chunkX, int chunkZ, IChunkGet chunk);

    public abstract ChunkFilterBlock init(IChunkGet iget, IChunkSet iset,
        int layer);

    public abstract void flood(IChunkGet iget, IChunkSet iset, int layer,
                               Flood flood, FilterBlockMask mask);


    public abstract void filter(Filter filter, int x, int y, int z);

    public abstract void filter(Filter filter, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ);

    public abstract void filter(Filter filter);

    public abstract void filter(Filter filter, int yStart, int yEnd);

    public abstract void filter(Filter filter, Region region);
}
