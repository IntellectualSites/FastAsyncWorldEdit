package com.fastasyncworldedit.core.math.random;

import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;

public class SimplexNoiseGenerator implements NoiseGenerator {

    @Override
    public float noise(Vector2 position) {
        return convert(SimplexNoise.noise(position.x(), position.z()));
    }

    @Override
    public float noise(Vector3 position) {
        return convert(SimplexNoise.noise(position.x(), position.y(), position.z()));
    }

    private float convert(double d) {
        // we need to go from [-1, 1] to [0, 1] and from double to float
        return (float) ((d + 1) * 0.5);
    }

}
