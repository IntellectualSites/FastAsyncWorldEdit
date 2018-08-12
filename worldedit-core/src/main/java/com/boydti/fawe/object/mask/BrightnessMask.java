package com.boydti.fawe.object.mask;

import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class BrightnessMask extends AbstractExtentMask {

    private final int min, max;

    public BrightnessMask(Extent extent, int min, int max) {
        super(extent);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(Vector vector) {
        Extent extent = getExtent();
        if (extent instanceof LightingExtent) {
            int light = ((LightingExtent) extent).getBrightness(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            return light >= min && light <= max;
        }
        return false;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
