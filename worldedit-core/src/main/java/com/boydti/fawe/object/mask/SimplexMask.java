package com.boydti.fawe.object.mask;

import com.boydti.fawe.object.random.SimplexNoise;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;

public class SimplexMask extends AbstractMask {
    private final double min, max, scale;

    public SimplexMask(double scale, double min, double max) {
        this.scale = scale;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(Vector vector) {
        double value = SimplexNoise.noise(vector.getBlockX() * scale, vector.getBlockY() * scale, vector.getBlockZ() * scale);
        return value >= min && value <= max;
    }
}
