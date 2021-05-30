package com.boydti.fawe.regions;

import com.sk89q.worldedit.regions.Region;

public class FaweMaskMultipleRegions extends FaweMask {

    private final Region[] regions;

    /**
     * Create a mask with multiple regions in it.
     *
     * @param regions Regions that will be masked
     */
    public FaweMaskMultipleRegions(Region[] regions) {
        super(null);

        this.regions = regions;
    }

    /**
     * NOT USE!!! Used by FaweMask to get the mask.
     *
     * @return first region on the regions array
     */
    @Deprecated
    @Override
    public Region getRegion() {
        return regions[0];
    }

    /**
     * Get the regions that will be masked.
     *
     * @return array of regions
     */
    public Region[] getRegions() {
        return regions;
    }
}
