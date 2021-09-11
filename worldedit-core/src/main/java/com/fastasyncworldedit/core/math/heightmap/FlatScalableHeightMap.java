package com.fastasyncworldedit.core.math.heightmap;

public class FlatScalableHeightMap extends ScalableHeightMap {

    /**
     * New height map where the returned height is the minmum height value if outside the size, otherwise returns height equal
     * to size.
     *
     * @param minY min y value allowed to be set. Inclusive.
     * @param maxY max y value allowed to be set. Inclusive.
     */
    public FlatScalableHeightMap(int minY, int maxY) {
        super(minY, maxY);
    }

    @Override
    public double getHeight(int x, int z) {
        int dx = Math.abs(x);
        int dz = Math.abs(z);
        int d2 = dx * dx + dz * dz;
        if (d2 > size2) {
            return minY;
        }
        return size;
    }

}
