package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HeightBoundExtent extends FaweRegionExtent implements IBatchProcessor {

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

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        if (trimY(set, min, max) | trimNBT(set, this::contains)) {
            return set;
        }
        return null;
    }
}
