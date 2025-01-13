package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;

public class AngleMask extends AbstractExtentMask implements ResettableMask {

    protected static double ADJACENT_MOD = 0.5;
    protected static double DIAGONAL_MOD = 1 / Math.sqrt(8);

    private final SolidBlockMask fastMask;
    protected final CachedMask mask;
    protected final double max;
    protected final double min;
    protected final boolean overlay;
    protected final boolean checkFirst;
    protected final int maxY;
    protected final int minY;
    protected final int distance;
    protected transient int cacheBotX = Integer.MIN_VALUE;
    protected transient int cacheBotZ = Integer.MIN_VALUE;
    protected transient short[] cacheHeights;
    protected transient int lastY;
    protected transient int lastX = Integer.MIN_VALUE;
    protected transient int lastZ = Integer.MIN_VALUE;
    protected transient boolean lastValue;

    public AngleMask(Extent extent, double min, double max, boolean overlay, int distance) {
        super(extent);
        this.fastMask = new SolidBlockMask(extent);
        this.mask = new CachedMask(this.fastMask);
        this.min = min;
        this.max = max;
        this.checkFirst = max >= (Math.tan(90 * (Math.PI / 180)));
        this.maxY = extent.getMaxY();
        this.minY = extent.getMinY();
        this.overlay = overlay;
        this.distance = distance;
    }

    @Override
    public void reset() {
        cacheBotX = Integer.MIN_VALUE;
        cacheBotZ = Integer.MIN_VALUE;
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        lastZ = Integer.MIN_VALUE;
        if (cacheHeights != null) {
            Arrays.fill(cacheHeights, (short) 0);
        }
    }

    protected int getHeight(Extent extent, int x, int y, int z) {
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
                cacheHeights = new short[65536];
                Arrays.fill(cacheHeights, (short) minY);
            } else {
                Arrays.fill(cacheHeights, (short) minY);
            }
        } else {
            index = rx + (rz << 8);
        }
        int result = cacheHeights[index];
        if (y > result) {
            cacheHeights[index] = (short) (result = lastY = extent.getNearestSurfaceTerrainBlock(x, z, lastY, minY, maxY));
        }
        return result;
    }

    protected boolean testSlope(Extent extent, int x, int y, int z) {
        double slope;
        lastY = y;
        slope =
                Math.abs(getHeight(extent, x + distance, y, z) - getHeight(extent, x - distance, y, z))
                        * ADJACENT_MOD;
        if (checkFirst) {
            if (slope >= min) {
                return lastValue = true;
            }
            slope = Math.max(
                    slope,
                    Math.abs(getHeight(extent, x, y, z + distance) - getHeight(extent, x, y, z - distance)) * ADJACENT_MOD
            );
            slope = Math.max(
                    slope,
                    Math.abs(
                            getHeight(extent, x + distance, y, z + distance) -
                                    getHeight(extent, x - distance, y, z - distance)) * DIAGONAL_MOD
            );
            slope = Math.max(
                    slope,
                    Math.abs(
                            getHeight(extent, x - distance, y, z + distance) -
                                    getHeight(extent, x + distance, y, z - distance)) * DIAGONAL_MOD
            );
            return lastValue = (slope >= min);
        } else {
            slope = Math.max(
                    slope,
                    Math.abs(getHeight(extent, x, y, z + distance) - getHeight(extent, x, y, z - distance)) * ADJACENT_MOD
            );
            slope = Math.max(
                    slope,
                    Math.abs(getHeight(extent, x + distance, y, z + distance) -
                            getHeight(extent, x - distance, y, z - distance)) * DIAGONAL_MOD
            );
            slope = Math.max(
                    slope,
                    Math.abs(getHeight(extent, x - distance, y, z + distance) -
                            getHeight(extent, x + distance, y, z - distance)) * DIAGONAL_MOD
            );
            return lastValue = (slope >= min && slope <= max);
        }
    }

    // for optimal performance, this method should be called with base being a CharFilterBlock
    private boolean adjacentAir(Extent extent, BlockVector3 base) {
        int y = base.y();
        // we expect the data for blocks above and below to be loaded already
        // in which case caching/reading from cache has more overhead
        if (y != maxY && !fastMask.test(base.getStateRelativeY(extent, 1))) {
            return true;
        }
        if (y != minY && !fastMask.test(base.getStateRelativeY(extent, -1))) {
            return true;
        }
        // other positions might be in different chunks, go a slower, cached path there
        MutableBlockVector3 mutable = new MutableBlockVector3();
        int x = base.x();
        int z = base.z();
        if (!mask.test(extent, mutable.setComponents(x + 1, y, z))) {
            return true;
        }
        if (!mask.test(extent, mutable.setComponents(x - 1, y, z))) {
            return true;
        }
        if (!mask.test(extent, mutable.setComponents(x, y, z + 1))) {
            return true;
        }
        return !mask.test(extent, mutable.setComponents(x, y, z - 1));
    }

    @Override
    public boolean test(BlockVector3 vector) {

        if (!fastMask.test(vector)) {
            return false;
        }
        int y = vector.y();
        if (overlay) {
            if (y < maxY && !adjacentAir(getExtent(), vector)) {
                return false;
            }
        }
        int x = vector.x();
        int z = vector.z();
        return testSlope(getExtent(), x, y, z);
    }

    @Override
    public boolean test(final Extent extent, final BlockVector3 vector) {
        int x = vector.x();
        int y = vector.y();
        int z = vector.z();

        if ((lastX == (lastX = x) & lastZ == (lastZ = z))) {
            int height = getHeight(extent, x, y, z);
            if (y <= height) {
                return overlay ? (lastValue && y == height) : lastValue;
            }
        }

        if (!fastMask.test(extent, vector)) {
            return false;
        }
        if (overlay) {
            if (y < maxY && !adjacentAir(extent, vector)) {
                return lastValue = false;
            }
        }
        return testSlope(extent, x, y, z);
    }

    @Override
    public Mask copy() {
        return new AngleMask(getExtent(), min, max, overlay, distance);
    }

}
