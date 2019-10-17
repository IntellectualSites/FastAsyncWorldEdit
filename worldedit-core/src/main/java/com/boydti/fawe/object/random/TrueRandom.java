package com.boydti.fawe.object.random;

import java.util.SplittableRandom;

public class TrueRandom implements SimpleRandom {

    private final SplittableRandom random = new SplittableRandom();

    @Override
    public double nextDouble(int x, int y, int z) {
        return random.nextDouble();
    }

    @Override
    public int nextInt(int x, int y, int z, int len) {
        return random.nextInt(len);
    }
}
