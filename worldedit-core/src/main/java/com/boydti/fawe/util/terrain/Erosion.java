package com.boydti.fawe.util.terrain;

import java.util.Arrays;

import static com.boydti.fawe.util.MathMan.pairInt;

public final class Erosion {
    private final int area;
    private float[][] terrainHeight;
    private float[][] waterHeight;

    private long[] queue_2;
    private long[] queue;
    private int queueIndex;

    public Erosion(int width, int length) {
        this.area = width * length;
        queue = new long[area];
        Arrays.fill(queue, -1);

    }

    public void addWater(int x, int z, float amt) {
        waterHeight[x][z] += amt;
        queue[queueIndex++] = pairInt(x, z);
    }

    public void propogateWater() {

    }


}
