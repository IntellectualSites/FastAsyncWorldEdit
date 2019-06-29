package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class HeightBoundExtent extends FaweRegionExtent {

    private final int min, max;
    private int lastY = -1;
    private boolean lastResult;

    public HeightBoundExtent(Extent extent, FaweLimit limit, int min, int max) {
        super(extent, limit);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean contains(int x, int z) {
        return true;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        if (y == lastY) {
            return lastResult;
        }
        lastY = y;
        return lastResult = (y >= min && y <= max);
    }

    @Override
    public Collection<Region> getRegions() {
        return Collections.singletonList(new RegionWrapper(Integer.MIN_VALUE, Integer.MAX_VALUE, min, max, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
}
