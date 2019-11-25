package com.boydti.fawe.object.mask;

import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.math.BlockVector3;

public class SkyLightMask extends AbstractExtentMask {

    private final int min;
    private final int max;

    public SkyLightMask(Extent extent, int min, int max) {
        super(extent);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (getExtent() instanceof LightingExtent) {
            int light = ((LightingExtent) getExtent())
                .getSkyLight(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            return light >= min && light <= max;
        }
        return false;
    }

}
