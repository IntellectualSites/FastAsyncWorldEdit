package com.boydti.fawe.beta;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;

public abstract class ChunkFilterBlock extends FilterBlock {
    public ChunkFilterBlock(Extent extent) {
        super(extent);
    }

    public abstract ChunkFilterBlock init(int X, int Z, IChunkGet chunk);

    public abstract void flood(final IChunkGet iget, final IChunkSet iset, final int layer, Flood flood, FilterBlockMask mask);

    public abstract void filter(IChunkGet get, IChunkSet set, int layer, Filter filter, @Nullable Region region, BlockVector3 min, BlockVector3 max);
}
