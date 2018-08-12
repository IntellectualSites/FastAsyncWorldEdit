package com.boydti.fawe.object.random;

public interface SimpleRandom {
    public double nextDouble(int x, int y, int z);

    public default int nextInt(int x, int y, int z, int len) {
        double val = nextDouble(x, y, z);
        return (int) (val * len);
    }
}
