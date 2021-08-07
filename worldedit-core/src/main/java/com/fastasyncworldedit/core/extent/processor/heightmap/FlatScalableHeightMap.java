package com.fastasyncworldedit.core.extent.processor.heightmap;

public class FlatScalableHeightMap extends ScalableHeightMap {

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
