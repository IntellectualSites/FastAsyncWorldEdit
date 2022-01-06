package com.fastasyncworldedit.core.math.heightmap;

public class ArrayHeightMap extends ScalableHeightMap {

    // The heights
    private final char[][] height;
    // The height map width/length
    private final int width;
    private final int length;
    private final double scale;
    // The size to width/length ratio
    private double rx;
    private double rz;

    /**
     * New height map represented by char array[][] of values x*z to be scaled given a set size.
     * Limited 0->65535
     *
     * @param height array of height values
     */
    public ArrayHeightMap(char[][] height, double scale) {
        setSize(5);
        this.height = height;
        this.width = height.length;
        this.length = height[0].length;
        this.scale = scale;
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
        return ((height[x][z] & 0xFFFF) * size) / scale;

    }

}
