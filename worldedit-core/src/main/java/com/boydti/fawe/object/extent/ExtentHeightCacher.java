package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.PassthroughExtent;

import java.util.Arrays;

public class ExtentHeightCacher extends PassthroughExtent {

    public ExtentHeightCacher(Extent extent) {
        super(extent);
    }

    public void reset() {
        cacheBotX = Integer.MIN_VALUE;
        cacheBotZ = Integer.MIN_VALUE;
        if (cacheHeights != null) {
            Arrays.fill(cacheHeights, (byte) 0);
        }
    }

    private transient int cacheBotX = Integer.MIN_VALUE;
    private transient int cacheBotZ = Integer.MIN_VALUE;
    private transient byte[] cacheHeights;
    private transient int lastY;

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
                cacheHeights = new byte[65536];
            } else {
                Arrays.fill(cacheHeights, (byte) 0);
            }
        } else {
            index = rx + (rz << 8);
        }
        int result = cacheHeights[index] & 0xFF;
        if (result == 0) {
            cacheHeights[index] = (byte) (result = lastY = super.getNearestSurfaceTerrainBlock(x, z, lastY, minY, maxY));
        }
        return result;
    }
}
