package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;

public class SummedAreaTable {
    private final char[] source;
    private final long[] summed;
    private final int length;
    private final int width;
    private final int area;
    private final int radius;
    private final float areaInverse;
    private final float[] areaInverses;

    public SummedAreaTable(long[] buffer, char[] matrix, int width, int radius) {
        this.source = matrix;
        this.summed = buffer;
        this.width = width;
        this.length = buffer.length / width;
        this.radius = radius;
        this.area = MathMan.sqr(radius * 2 + 1);
        this.areaInverse = 1f / area;
        this.areaInverses = new float[area - 2];
        for (int area = 2; area < this.area; area++) {
            this.areaInverses[area - 2] = 1f / area;
        }
    }

    public void processSummedAreaTable() {
        int rowSize = source.length / width;
        int colSize = width;
        int index = 0;
        for (int i = 0; i < rowSize; i++) {
            for (int j = 0; j < colSize; j++, index++) {
                long val = getVal(i, j, index, source[index]);
                summed[index] = val;
            }
        }
    }

    private long getSum(int index) {
        if (index < 0) return 0;
        return summed[index];
    }

    public int average(int x, int z, int index) {
        int minX = Math.max(0, x - radius) - x;
        int minZ = Math.max(0, z - radius) - z;
        int maxX = Math.min(width - 1, x + radius) - x;
        int maxZ = Math.min(length - 1, z + radius) - z;
        int maxzwi = maxZ * width;
        int XZ = index + maxzwi + maxX;
        int area = (maxX - minX + 1) * (maxZ - minZ + 1);

        long total = getSum(XZ);

        int minzw = minZ * width;
        int Z = index + minzw + maxX;
        if (x > radius) {
            int X = index + minX + maxzwi;
            int M = index + minzw + minX;
            total -= summed[X - 1];
            total += getSum(M - width - 1);
        }
        total -= getSum(Z - width);
        if (area == this.area) {
            return (int) (total * areaInverse);
        } else {
            return Math.round(total * areaInverses[area - 2]);
        }
    }

    private long getVal(int row, int col, int index, long curr) {
        long leftSum;                    // sub matrix sum of left matrix
        long topSum;                        // sub matrix sum of top matrix
        long topLeftSum;                    // sub matrix sum of top left matrix
        /* top left value is itself */
        if (index == 0) {
            return curr;
        }
		/* top row */
        else if (row == 0 && col != 0) {
            leftSum = summed[index - 1];
            return curr + leftSum;
        }
		/* left-most column */
        else if (row != 0 && col == 0) {
            topSum = summed[index - width];
            return curr + topSum;
        } else {
            leftSum = summed[index - 1];
            topSum = summed[index - width];
            topLeftSum = summed[index - width - 1]; // overlap between leftSum and topSum
            return curr + leftSum + topSum - topLeftSum;
        }
    }
}