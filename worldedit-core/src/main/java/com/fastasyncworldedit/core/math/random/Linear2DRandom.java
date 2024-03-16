package com.fastasyncworldedit.core.math.random;

import static com.fastasyncworldedit.core.math.random.Linear3DRandom.doubleDiv;
import static java.lang.Math.floorDiv;

/**
 * A {@link SimpleRandom} that deterministically maps coordinates
 * to values.
 * @since TODO
 */
public class Linear2DRandom implements SimpleRandom {
    private final int xScale;
    private final int zScale;

    /**
     * Creates a new {@link Linear2DRandom} instance
     *
     * @param xScale the scale applied to the x component of a coordinate
     * @param zScale the scale applied to the z component of a coordinate
     */
    public Linear2DRandom(final int xScale, final int zScale) {
        this.xScale = xScale;
        this.zScale = zScale;
    }

    @Override
    public double nextDouble(final int x, final int y, final int z) {
        return nextDouble(x, y, z, 1d);
    }

    @Override
    public double nextDouble(final int x, final int y, final int z, double bound) {
        double index = (doubleDiv(x, this.xScale) + doubleDiv(z, this.zScale)) % bound;
        if (index < 0) {
            index += bound;
        }
        return index;

    }

    @Override
    public int nextInt(final int x, final int y, final int z, final int bound) {
        int index = (floorDiv(x, this.xScale) + floorDiv(z, this.zScale)) % bound;
        if (index < 0) {
            index += bound;
        }
        return index;
    }

}
