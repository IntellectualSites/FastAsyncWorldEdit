package com.boydti.fawe.object.brush.heightmap;

public class ArrayHeightMap extends ScalableHeightMap {
    // The heights
    private final byte[][] height;
    // The height map width/length
    private final int width, length;
    // The size to width/length ratio
    private double rx, rz;

    public ArrayHeightMap(byte[][] height) {
        setSize(5);
        this.height = height;
        this.width = height.length;
        this.length = height[0].length;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        this.rx = (double) width / (size << 1);
        this.rz = (double) length / (size << 1);

    }

    @Override
    public double getHeight(int x, int z) {
        x = (int) Math.max(0, Math.min(width - 1, (x + size) * rx));
        z = (int) Math.max(0, Math.min(length - 1, (z + size) * rz));
        return ((height[x][z] & 0xFF) * size) / 256d;

    }
}
