package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.extent.Extent;

public interface LightingExtent extends Extent {
    int getLight(int x, int y, int z);

    int getSkyLight(int x, int y, int z);

    int getBlockLight(int x, int y, int z);

    int getOpacity(int x, int y, int z);

    int getBrightness(int x, int y, int z);
}
