package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweLimit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
}
