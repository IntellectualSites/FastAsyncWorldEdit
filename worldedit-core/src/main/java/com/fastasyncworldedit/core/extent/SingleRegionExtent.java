package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;

public final class SingleRegionExtent extends FaweRegionExtent {

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
    public Future<?> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        // Most likely will do nothing, but perhaps people will find some fun way of using this via API (though doubtful)
        return region.postProcessSet(chunk, get, set);
    }

    @Override
    public void postProcess(IChunk chunk, IChunkGet get, IChunkSet set) {
        // Most likely will do nothing, but perhaps people will find some fun way of using this via API (though doubtful)
        region.postProcess(chunk, get, set);
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        return region.containsChunk(chunkX, chunkZ);
    }

}
