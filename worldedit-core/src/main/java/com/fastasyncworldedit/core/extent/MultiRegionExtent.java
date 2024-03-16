package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.regions.RegionWrapper;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public final class MultiRegionExtent extends FaweRegionExtent {

    @Nullable
    private final RegionIntersection intersection;
    @Nullable
    private final RegionIntersection disallowedIntersection;
    @Nullable
    private final Region[] allowed;
    @Nullable
    private final Region[] disallowed;
    @Nullable
    private Region region;
    private int index;

    /**
     * Create a new instance. Has both allowed and disallowed regions. Assumes that disallowed regions are encompassed by
     * allowed regions.
     *
     * @param extent     the extent
     * @param limit      the limit to be used
     * @param allowed    the allowed regions or null for global editing
     * @param disallowed the disallowed regions or null for no disallowed regions
     */
    public MultiRegionExtent(Extent extent, FaweLimit limit, @Nullable Region[] allowed, @Nullable Region[] disallowed) {
        super(extent, limit);
        this.index = 0;
        if (allowed != null && !allowed[0].isGlobal()) {
            this.region = allowed[0];
            this.allowed = allowed;
            this.intersection = new RegionIntersection(Arrays.asList(allowed));
        } else {
            this.region = null;
            this.allowed = null;
            this.intersection = null;
        }
        if (disallowed != null && disallowed.length > 0) {
            this.disallowed = disallowed;
            this.disallowedIntersection = new RegionIntersection(Arrays.asList(disallowed));
        } else {
            this.disallowed = null;
            this.disallowedIntersection = null;
        }
    }

    @Override
    public boolean contains(int x, int y, int z) {
        if (region != null && region.contains(x, y, z)) {
            if (disallowed != null) {
                for (final Region disallow : disallowed) {
                    if (disallow.contains(x, y, z)) {
                        return false;
                    }
                }
            }
            return true;
        }
        boolean result = allowed == null;
        if (!result) {
            for (int i = 0; i < allowed.length; i++) {
                if (i != index) {
                    Region current = allowed[i];
                    if (current.contains(x, y, z)) {
                        region = current;
                        index = i;
                        result = true;
                        break;
                    }
                }
            }
        }
        if (!result || disallowed == null) {
            return result;
        }
        for (final Region disallow : disallowed) {
            if (disallow.contains(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(int x, int z) {
        if (region != null && region.contains(x, z)) {
            if (disallowed != null) {
                for (final Region disallow : disallowed) {
                    if (disallow.contains(x, z)) {
                        return false;
                    }
                }
            }
            return true;
        }
        boolean result = allowed == null;
        if (!result) {
            for (int i = 0; i < allowed.length; i++) {
                if (i != index) {
                    Region current = allowed[i];
                    if (current.contains(x, z)) {
                        region = current;
                        index = i;
                        result = true;
                        break;
                    }
                }
            }
        }
        if (!result || disallowed == null) {
            return result;
        }
        for (final Region disallow : disallowed) {
            if (disallow.contains(x, z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all allowed regions
     */
    @Override
    public Collection<Region> getRegions() {
        if (allowed == null) {
            return List.of(RegionWrapper.GLOBAL());
        }
        return Arrays.asList(allowed);
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        boolean result = allowed == null;
        if (!result) {
            for (Region region : allowed) {
                if (region.containsChunk(chunkX, chunkZ)) {
                    result = true;
                    break;
                }
            }
        }
        if (!result || disallowed == null) {
            return result;
        }
        for (Region region : disallowed) {
            if (region.containsChunk(chunkX, chunkZ)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        if (intersection != null) {
            set = intersection.processSet(chunk, get, set);
        }
        if (disallowedIntersection != null) {
            set = disallowedIntersection.processSet(chunk, get, set, true);
        }
        return set;
    }

    @Override
    public Future<?> postProcessSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return intersection.postProcessSet(chunk, get, set);
    }

    @Override
    public void postProcess(IChunk chunk, IChunkGet get, IChunkSet set) {
        intersection.postProcess(chunk, get, set);
    }

}
