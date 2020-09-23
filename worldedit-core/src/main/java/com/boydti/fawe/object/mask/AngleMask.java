package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

import java.util.Arrays;

public class AngleMask extends SolidBlockMask implements ResettableMask {
    public static double ADJACENT_MOD = 0.5;
    public static double DIAGONAL_MOD = 1 / Math.sqrt(8);

    protected final CachedMask mask;
    protected final double max;
    protected final double min;
    protected final boolean overlay;
    protected final boolean checkFirst;
    protected final int maxY;
    protected final int distance;

    protected transient MutableBlockVector3 mutable = new MutableBlockVector3();

    public AngleMask(Extent extent, double min, double max, boolean overlay, int distance) {
        super(extent);
        this.mask = new CachedMask(new SolidBlockMask(extent));
        this.min = min;
        this.max = max;
        this.checkFirst = max >= (Math.tan(90 * (Math.PI / 180)));
        this.maxY = extent.getMaximumPoint().getBlockY();
        this.overlay = overlay;
        this.distance = distance;
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector3();
        cacheBotX = Integer.MIN_VALUE;
        cacheBotZ = Integer.MIN_VALUE;
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        if (cacheHeights != null) {
            Arrays.fill(cacheHeights, (byte) 0);
        }
    }

    protected transient int cacheCenZ;
    protected transient int cacheBotX = Integer.MIN_VALUE;
    protected transient int cacheBotZ = Integer.MIN_VALUE;
    protected transient int cacheCenterZ;

    protected transient byte[] cacheHeights;

    protected transient int lastY;
    protected transient int lastX = Integer.MIN_VALUE;
    protected transient int lastZ = Integer.MIN_VALUE;
    protected transient boolean foundY;
    protected transient boolean lastValue;

    public int getHeight(Extent extent, int x, int y, int z) {
        //        return extent.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
        try {
            int rx = x - cacheBotX + 16;
            int rz = z - cacheBotZ + 16;
            int index;
            if (((rx & 0xFF) != rx || (rz & 0xFF) != rz)) {
                cacheBotX = x - 16;
                cacheBotZ = z - 16;
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
            if (y > result) {
                cacheHeights[index] = (byte) (result = lastY = extent.getNearestSurfaceTerrainBlock(x, z, lastY, 0, maxY));
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    protected boolean testSlope(Extent extent, int x, int y, int z) {
        double slope;
        boolean aboveMin;
        lastY = y;
        slope =
                Math.abs(getHeight(extent, x + distance, y, z) - getHeight(extent, x - distance, y, z))
                        * ADJACENT_MOD;
        if (checkFirst) {
            if (slope >= min) {
                return lastValue = true;
            }
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x, y, z + distance) - getHeight(extent, x, y, z - distance))
                    * ADJACENT_MOD);
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x + distance, y, z + distance) - getHeight(extent,
                            x - distance, y, z - distance)) * DIAGONAL_MOD);
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x - distance, y, z + distance) - getHeight(extent,
                            x + distance, y, z - distance)) * DIAGONAL_MOD);
            return lastValue = (slope >= min);
        } else {
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x, y, z + distance) - getHeight(extent, x, y, z - distance))
                    * ADJACENT_MOD);
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x + distance, y, z + distance) - getHeight(extent,
                            x - distance, y, z - distance)) * DIAGONAL_MOD);
            slope = Math.max(slope, Math.abs(
                    getHeight(extent, x - distance, y, z + distance) - getHeight(extent,
                            x + distance, y, z - distance)) * DIAGONAL_MOD);
            return lastValue = (slope >= min && slope <= max);
        }
    }

    public boolean adjacentAir(BlockVector3 v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        if (!mask.test(x + 1, y, z)) {
            return true;
        }
        if (!mask.test(x - 1, y, z)) {
            return true;
        }
        if (!mask.test(x, y, z + 1)) {
            return true;
        }
        if (!mask.test(x, y, z - 1)) {
            return true;
        }
        if (y < 255 && !mask.test(x, y + 1, z)) {
            return true;
        }
        if (y > 0 && !mask.test(x, y - 1, z)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int x = vector.getBlockX();
        int y = vector.getBlockY();
        int z = vector.getBlockZ();

        if ((lastX == (lastX = x) & lastZ == (lastZ = z))) {
            int height = getHeight(getExtent(), x, y, z);
            if (y <= height) {
                return overlay ? (lastValue && y == height) : lastValue;
            }
        }

        if (!mask.test(x, y, z)) {
            return false;
        }
        if (overlay) {
            if (y < 255 && !adjacentAir(vector)) {
                return lastValue = false;
            }
        }
        return testSlope(getExtent(), x, y, z);
    }

    @Override
    public Mask copy() {
        return new AngleMask(getExtent(), min, max, overlay, distance);
    }

}
