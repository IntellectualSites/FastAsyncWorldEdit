package com.sk89q.worldedit.math.noise;

import com.fastasyncworldedit.core.object.random.SimplexNoise;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;

public class SimplexNoiseGenerator implements NoiseGenerator {

    @Override
    public float noise(Vector2 position) {
        return convert(SimplexNoise.noise(position.getX(), position.getZ()));
    }

    @Override
    public float noise(Vector3 position) {
        return convert(SimplexNoise.noise(position.getX(), position.getY(), position.getZ()));
    }

    private float convert(double d) {
        // we need to go from [-1, 1] to [0, 1] and from double to float
        return (float) ((d + 1) * 0.5);
    }
}
