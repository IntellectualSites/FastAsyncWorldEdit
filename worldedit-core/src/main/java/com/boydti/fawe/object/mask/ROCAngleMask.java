package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;

public class ROCAngleMask extends AngleMask {

    public ROCAngleMask(Extent extent, double min, double max, boolean overlay, int distance) {
        super(extent, min, max, overlay, distance);
    }

    @Override
    protected boolean testSlope(int x, int y, int z) {
        double tmp;
        lastY = y;

        int base = getHeight(x, y, z);
        double slope =
            (getHeight(x + distance, y, z) - base - (base - getHeight(x - distance, y, z)))
                * ADJACENT_MOD;

        tmp = (getHeight(x, y, z + distance) - base - (base - getHeight(x, y, z - distance))) * ADJACENT_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) slope = tmp;

        tmp = (getHeight(x + distance, y, z + distance) - base - (base - getHeight(x - distance, y,
            z - distance))) * DIAGONAL_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) slope = tmp;

        tmp = (getHeight(x - distance, y, z + distance) - base - (base - getHeight(x + distance, y,
            z - distance))) * DIAGONAL_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) slope = tmp;

        return lastValue = slope >= min && slope <= max;
    }
}
