package com.fastasyncworldedit.core.extent;

import com.sk89q.worldedit.extent.Extent;

import java.util.Arrays;

public final class ExtentHeightCacher extends PassthroughExtent {

    private transient int cacheBotX = Integer.MIN_VALUE;
    private transient int cacheBotZ = Integer.MIN_VALUE;
    private transient short[] cacheHeights;
    private transient int lastY;

    public ExtentHeightCacher(Extent extent) {
        super(extent);
    }

    public void reset() {
        cacheBotX = Integer.MIN_VALUE;
        cacheBotZ = Integer.MIN_VALUE;
        if (cacheHeights != null) {
            Arrays.fill(cacheHeights, (short) 0);
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        int rx = x - cacheBotX + 16;
        int rz = z - cacheBotZ + 16;
        int index;
        if ((rx & 0xFF) != rx || (rz & 0xFF) != rz) {
            cacheBotX = x - 16;
            cacheBotZ = z - 16;
            lastY = y;
            rx = x - cacheBotX + 16;
            rz = z - cacheBotZ + 16;
            index = rx + (rz << 8);
            if (cacheHeights == null) {
                cacheHeights = new short[65536];
                Arrays.fill(cacheHeights, (short) minY);
            } else {
                Arrays.fill(cacheHeights, (short) minY);
            }
        } else {
            index = rx + (rz << 8);
        }
        int result = cacheHeights[index] & 0xFF;
        if (result == minY) {
            cacheHeights[index] = (short) (result = lastY = super
                    .getNearestSurfaceTerrainBlock(x, z, lastY, minY, maxY));
        }
        return result;
    }

}
