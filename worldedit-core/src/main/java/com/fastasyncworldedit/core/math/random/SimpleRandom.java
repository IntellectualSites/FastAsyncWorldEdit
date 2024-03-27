package com.fastasyncworldedit.core.math.random;

public interface SimpleRandom {

    /**
     * Generate a random double from three integer components.
     * The generated value is between 0 (inclusive) and 1 (exclusive).
     *
     * @param x the first component
     * @param y the second component
     * @param z the third component
     * @return a double between 0 (inclusive) and 1 (exclusive)
     */
    double nextDouble(int x, int y, int z);

    /**
     * Generate a random double from three integer components.
     * The generated value is between 0 (inclusive) and {@code bound} (exclusive).
     *
     * @param x     the first component
     * @param y     the second component
     * @param z     the third component
     * @param bound upper bound (exclusive)
     * @return a double between 0 (inclusive) and {@code bound} (exclusive)
     */
    default double nextDouble(int x, int y, int z, double bound) {
        return nextDouble(x, y, z) * bound;
    }

    /**
     * Generate a random integer from three integer components.
     * The generated value is between 0 (inclusive) and 1 (exclusive)
     *
     * @param x     the first component
     * @param y     the second component
     * @param z     the third component
     * @param bound the upper bound (exclusive)
     * @return a random integer between 0 (inclusive) and {@code bound} (exclusive)
     */
    default int nextInt(int x, int y, int z, int bound) {
        double val = nextDouble(x, y, z, bound);
        return (int) val;
    }

}
