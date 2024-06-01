package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.random.SimplexNoise;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class SimplexMask extends AbstractMask {

    private final double min;
    private final double max;
    private final double scale;

    public SimplexMask(double scale, double min, double max) {
        this.scale = scale;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        double value = SimplexNoise.noise(vector.x() * scale, vector.y() * scale, vector.z() * scale);
        return value >= min && value <= max;
    }

    @Override
    public Mask copy() {
        // The mask is not mutable. There is no need to clone it.
        return this;
    }

}
