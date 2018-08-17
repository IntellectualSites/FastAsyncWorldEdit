package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class SummedColorTable {
    private static float inv256 = 1/256f;
    private final long[] reds, greens, blues, alpha;
    private final int[] hasAlpha;
    private final int length;
    private final int width;
    private final float[] areaInverses;
    private final float[] alphaInverse;

    public SummedColorTable(BufferedImage image, final boolean calculateAlpha) {
        int[] raw = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.width = image.getWidth();
        this.length = image.getHeight();

        this.reds = new long[raw.length];
        this.greens = new long[raw.length];
        this.blues = new long[raw.length];
        this.hasAlpha = new int[raw.length];
        this.alpha = calculateAlpha ? new long[raw.length] : null;
        this.alphaInverse = calculateAlpha ? new float[256] : null;
        this.areaInverses = new float[1024 * 1024]; // 1 MB should be enough to cover scaling
        for (int i = 0; i < areaInverses.length; i++) {
            areaInverses[i] = 1f / (i + 1);
        }

        int index = 0;
        if (calculateAlpha) {
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < width; j++, index++) {
                    int color = raw[index];
                    int alpha = (color >> 24) & 0xFF;
                    int red, green, blue;
                    switch (alpha) {
                        case 0:
                            red = green = blue = 0;
                            break;
                        case 255:
                            red = (color >> 16) & 0xFF;
                            green = (color >> 8) & 0xFF;
                            blue = (color >> 0) & 0xFF;
                            break;
                        default:
                            red = (((color >> 16) & 0xFF) * alpha) >> 8;
                            green = (((color >> 8) & 0xFF) * alpha) >> 8;
                            blue = (((color >> 0) & 0xFF) * alpha) >> 8;
                            break;
                    }
                    this.reds[index] = getVal(i, j, index, red, this.reds);
                    this.greens[index] = getVal(i, j, index, green, this.greens);
                    this.blues[index] = getVal(i, j, index, blue, this.blues);
                    this.alpha[index] = getVal(i, j, index, alpha, this.alpha);
                    this.hasAlpha[index] = getVal(i, j, index, alpha > 0 ? 1 : 0, this.hasAlpha);
                }
            }
            for (int i = 1; i < alphaInverse.length; i++) {
                alphaInverse[i] = 256f / i;
            }
        } else {
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < width; j++, index++) {
                    int color = raw[index];
                    int red, green, blue, alpha;
                    if (((color >> 24) != 0)) {
                        alpha = 1;
                        red = (color >> 16) & 0xFF;
                        green = (color >> 8) & 0xFF;
                        blue = (color >> 0) & 0xFF;
                    } else {
                        alpha = red = green = blue = 0;
                    }
                    this.reds[index] = getVal(i, j, index, red, this.reds);
                    this.greens[index] = getVal(i, j, index, green, this.greens);
                    this.blues[index] = getVal(i, j, index, blue, this.blues);
                    this.hasAlpha[index] = getVal(i, j, index, alpha, this.hasAlpha);

                }
            }
        }
    }

    private long getSum(int index, long[] summed) {
        if (index < 0) return 0;
        return summed[index];
    }

    public int averageRGB(int x1, int z1, int x2, int z2) {
        int minX = Math.max(0, x1);
        int minZ = Math.max(0, z1);
        int maxX = Math.min(width - 1, x2);
        int maxZ = Math.min(length - 1, z2);

        int XZ = maxZ * width + maxX;
        long totRed = reds[XZ];
        long totGreen = greens[XZ];
        long totBlue = blues[XZ];
        int area = hasAlpha[XZ];

        if (minX > 0) {
            int pos = maxZ * width + minX - 1;
            totRed -= reds[pos];
            totGreen -= greens[pos];
            totBlue -= blues[pos];
            area -= hasAlpha[pos];
        }
        if (minZ > 0) {
            int pos = minZ * width - width + maxX;
            totRed -= reds[pos];
            totGreen -= greens[pos];
            totBlue -= blues[pos];
            area -= hasAlpha[pos];
        }

        if (minZ > 0 && minX > 0) {
            int pos = minZ * width - width + minX - 1;
            totRed += reds[pos];
            totGreen += greens[pos];
            totBlue += blues[pos];
            area += hasAlpha[pos];
        }

        if (area == 0) return 0;
        float factor = this.areaInverses[area - 1];
        return (255 << 24) + (((int) (totRed * factor)) << 16) + (((int) (totGreen * factor)) << 8) + (((int) (totBlue * factor)) << 0);
    }

    public int averageRGBA(int x1, int z1, int x2, int z2) {
        int minX = Math.max(0, x1);
        int minZ = Math.max(0, z1);
        int maxX = Math.min(width - 1, x2);
        int maxZ = Math.min(length - 1, z2);

        int XZ = maxZ * width + maxX;
        long totRed = reds[XZ];
        long totGreen = greens[XZ];
        long totBlue = blues[XZ];
        long totAlpha = alpha[XZ];
        int area = hasAlpha[XZ];

        if (minX > 0) {
            int pos = maxZ * width + minX - 1;
            totRed -= reds[pos];
            totGreen -= greens[pos];
            totBlue -= blues[pos];
            totAlpha -= alpha[pos];
            area -= hasAlpha[pos];
        }
        if (minZ > 0) {
            int pos = minZ * width - width + maxX;
            totRed -= reds[pos];
            totGreen -= greens[pos];
            totBlue -= blues[pos];
            totAlpha -= alpha[pos];
            area -= hasAlpha[pos];
        }

        if (minZ > 0 && minX > 0) {
            int pos = minZ * width - width + minX - 1;
            totRed += reds[pos];
            totGreen += greens[pos];
            totBlue += blues[pos];
            totAlpha += alpha[pos];
            area += hasAlpha[pos];
        }

        if (totAlpha == 0) return 0;

        float factor = this.areaInverses[area - 1];
        float alpha = (totAlpha * factor);
        factor = (factor * 256) / alpha;
        return (MathMan.clamp((int) alpha, 0, 255) << 24) + (((int) (totRed * factor)) << 16) + (((int) (totGreen * factor)) << 8) + (((int) (totBlue * factor)) << 0);
    }

    private long getVal(int row, int col, int index, long curr, long[] summed) {
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

    private int getVal(int row, int col, int index, int curr, int[] summed) {
        int leftSum;                    // sub matrix sum of left matrix
        int topSum;                        // sub matrix sum of top matrix
        int topLeftSum;                    // sub matrix sum of top left matrix
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