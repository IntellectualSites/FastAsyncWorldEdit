package com.fastasyncworldedit.core.math.random;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;

public class NoiseRandom implements SimpleRandom {

    private final NoiseGenerator generator;
    private final double scale;

    /**
     * Create a new NoiseRandom instance using a specific {@link NoiseGenerator} and a scale.
     *
     * @param generator The generator to use for the noise
     * @param scale     The scale of the noise
     */
    public NoiseRandom(NoiseGenerator generator, double scale) {
        this.generator = generator;
        this.scale = scale;
    }

    @Override
    public double nextDouble(int x, int y, int z) {
        return cap(this.generator.noise(Vector3.at(x, y, z).multiply(this.scale)));
    }

    // workaround for noise generators returning [0, 1]
    private double cap(double d) {
        if (d >= 1.0) {
            return 0x1.fffffffffffffp-1; // very close to 1 but less
        }
        return d;
    }
}
