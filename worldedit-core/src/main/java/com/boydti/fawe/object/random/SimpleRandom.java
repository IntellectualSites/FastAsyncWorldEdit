package com.boydti.fawe.object.random;

public interface SimpleRandom {

    double nextDouble(int x, int y, int z);

    default int nextInt(int x, int y, int z, int len) {
        double val = nextDouble(x, y, z);
        return (int) (val * len);
    }
}
