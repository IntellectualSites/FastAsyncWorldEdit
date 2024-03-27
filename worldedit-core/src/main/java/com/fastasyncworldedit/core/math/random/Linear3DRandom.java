package com.fastasyncworldedit.core.math.random;

import static java.lang.Math.floorDiv;

/**
 * A {@link SimpleRandom} that deterministically maps coordinates
 * to values.
 * @since TODO
 */
public class Linear3DRandom implements SimpleRandom {

    private final int xScale;
    private final int yScale;
    private final int zScale;

    /**
     * Creates a new {@link Linear3DRandom} instance
     *
     * @param xScale the scale applied to the x component of a coordinate
     * @param yScale the scale applied to the y component of a coordinate
     * @param zScale the scale applied to the z component of a coordinate
     */
    public Linear3DRandom(final int xScale, final int yScale, final int zScale) {
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    @Override
    public double nextDouble(final int x, final int y, final int z) {
        return nextDouble(x, y, z, 1d);
    }

    @Override
    public double nextDouble(final int x, final int y, final int z, double bound) {
        double index = (doubleDiv(x, this.xScale) + doubleDiv(y, this.yScale) + doubleDiv(z, this.zScale)) % bound;
        if (index < 0) {
            index += bound;
        }
        return index;
    }

    // used to avoid explicit conversion at call site
    static double doubleDiv(double dividend, double divisor) {
        // add a minimal value to avoid too many integral values hitting the exact weight of an entry in SimpleRandomCollection
        return Math.nextUp(dividend) / divisor;
    }

    @Override
    public int nextInt(final int x, final int y, final int z, final int bound) {
        int index = (floorDiv(x, this.xScale) + floorDiv(y, this.yScale) + floorDiv(z, this.zScale)) % bound;
        if (index < 0) {
            index += bound;
        }
        return index;
    }

}
