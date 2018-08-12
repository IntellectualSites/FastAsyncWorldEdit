package com.boydti.fawe.object.random;

public class SimplexRandom implements SimpleRandom {
    private final double scale;

    public SimplexRandom(double scale) {
        this.scale = scale;
    }

    @Override
    public double nextDouble(int x, int y, int z) {
        return (SimplexNoise.noise(x * scale, y * scale, z * scale) + 1) * 0.5;
    }
}
