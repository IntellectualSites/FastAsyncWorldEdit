package com.boydti.fawe.regions;

import com.sk89q.worldedit.regions.Region;

public class FaweMaskMultipleRegions extends FaweMask {

    private final Region[] regions;

    public FaweMaskMultipleRegions(Region[] regions) {
        super(null);

        this.regions = regions;
    }

    @Deprecated
    @Override
    public Region getRegion() {
        return regions[0];
    }

    public Region[] getRegions() {
        return regions;
    }
}
