package com.boydti.fawe.object.brush.heightmap;

public class AverageHeightMapFilter {
    private int[] inData;
    private int[] buffer;
    private final int width;
    private final int height;
    private final int minY;
    private final int maxY;

    public AverageHeightMapFilter(int[] inData, int width, int height, int minY, int maxY) {
        this.inData = inData;
        this.width = width;
        this.height = height;
        this.minY = minY;
        this.maxY = maxY;
        this.buffer = new int[inData.length];
    }

    public int[] filter(int iterations) {
        for (int j = 0; j < iterations; j++) {
            int a = -width;
            int b = width;
            int c = 1;
            int d = -1;
            for (int i = 0; i < inData.length; i++, a++, b++, c++, d++) {
                int height = inData[i];
                if (height < minY || height > maxY) {
                    buffer[i] = height;
                    continue;
                }
                int average = (2 + get(a, height) + get(b, height) + get(c, height) + get(d, height)) >> 2;
                buffer[i] = average;
            }
            int[] tmp = inData;
            inData = buffer;
            buffer = tmp;
        }
        return inData;
    }

    private int get(int index, int def) {
        int val = inData[Math.max(0, Math.min(inData.length - 1, index))];
        if (val < minY || val > maxY) {
            return def;
        }
        return val;
    }
}
