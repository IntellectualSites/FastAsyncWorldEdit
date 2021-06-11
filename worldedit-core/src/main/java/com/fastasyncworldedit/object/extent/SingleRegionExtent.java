package com.fastasyncworldedit.object.extent;

import com.fastasyncworldedit.beta.IChunk;
import com.fastasyncworldedit.beta.IChunkGet;
import com.fastasyncworldedit.beta.IChunkSet;
import com.fastasyncworldedit.object.FaweLimit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;

public class SingleRegionExtent extends FaweRegionExtent {

    private final Region region;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public SingleRegionExtent(Extent extent, FaweLimit limit, Region region) {
        super(extent, limit);
        this.region = region;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return region.contains(x, y, z);
    }

    @Override
    public boolean contains(int x, int z) {
        return region.contains(x, z);
    }

    @Override
    public Collection<Region> getRegions() {
        return Collections.singletonList(region);
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return region.processSet(chunk, get, set);
    }

    @Override
    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return region.postProcessSet(chunk, get, set);
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        return region.containsChunk(chunkX, chunkZ);
    }
}
