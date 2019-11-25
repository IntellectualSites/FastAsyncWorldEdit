package com.boydti.fawe.object.extent;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.FaweLimit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;

import java.util.Arrays;
import java.util.Collection;

public class MultiRegionExtent extends FaweRegionExtent {

    private final RegionIntersection intersection;
    private Region region;
    private final Region[] regions;
    private int index;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public MultiRegionExtent(Extent extent, FaweLimit limit, Region[] regions) {
        super(extent, limit);
        this.index = 0;
        this.region = regions[0];
        this.regions = regions;
        this.intersection = new RegionIntersection(Arrays.asList(regions));
    }

    @Override
    public boolean contains(int x, int y, int z) {
        if (region.contains(x, y, z)) {
            return true;
        }
        for (int i = 0; i < regions.length; i++) {
            if (i != index) {
                Region current = regions[i];
                if (current.contains(x, y, z)) {
                    region = current;
                    index = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        for (Region region : regions) {
            if (region.containsChunk(chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(int x, int z) {
        if (region.contains(x, z)) {
            return true;
        }
        for (int i = 0; i < regions.length; i++) {
            if (i != index) {
                Region current = regions[i];
                if (current.contains(x, z)) {
                    region = current;
                    index = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<Region> getRegions() {
        return Arrays.asList(regions);
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return intersection.processSet(chunk, get, set);
    }
}
