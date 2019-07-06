package com.boydti.fawe.object;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

@Deprecated
public class RegionWrapper extends CuboidRegion {
    private final static RegionWrapper GLOBAL = new RegionWrapper(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;

    public static RegionWrapper GLOBAL() {
        return GLOBAL;
    }

    public RegionWrapper(final int minX, final int maxX, final int minZ, final int maxZ) {
        this(minX, maxX, 0, 255, minZ, maxZ);
    }

    public RegionWrapper(final int minX, final int maxX, final int minY, final int maxY, final int minZ, final int maxZ) {
        this(BlockVector3.at(minX, 0, minZ), BlockVector3.at(maxX, 255, maxZ));
    }

    public RegionWrapper(final BlockVector3 pos1, final BlockVector3 pos2) {
        super(pos1, pos2);
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    @Override
    protected void recalculate() {
        super.recalculate();
        BlockVector3 pos1 = getMinimumPoint();
        BlockVector3 pos2 = getMaximumPoint();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    public RegionWrapper[] toArray() {
        return new RegionWrapper[]{this};
    }

    public boolean isIn(int x, int y, int z) {
        return (x >= this.minX) && (x <= this.maxX) && (z >= this.minZ) && (z <= this.maxZ) && (y >= this.minY) && (y <= this.maxY);
    }

    public boolean isInChunk(int cx, int cz) {
        int bx = cx << 4;
        int bz = cz << 4;
        int tx = bx + 15;
        int tz = bz + 15;
        return ((tx >= this.minX) && (bx <= this.maxX) && (tz >= this.minZ) && (bz <= this.maxZ));
    }

    public boolean isInMCA(int mcaX, int mcaZ) {
        int bx = mcaX << 9;
        int bz = mcaZ << 9;
        int tx = bx + 511;
        int tz = bz + 511;
        return ((tx >= this.minX) && (bx <= this.maxX) && (tz >= this.minZ) && (bz <= this.maxZ));
    }

    public boolean isIn(final int x, final int z) {
        return ((x >= this.minX) && (x <= this.maxX) && (z >= this.minZ) && (z <= this.maxZ));
    }

    public int distanceX(int x) {
        if (x >= minX) {
            if (x <= maxX) {
                return 0;
            }
            return maxX - x;
        }
        return minX - x;
    }

    public int distanceZ(int z) {
        if (z >= minZ) {
            if (z <= maxZ) {
                return 0;
            }
            return maxZ - z;
        }
        return minZ - z;
    }

    public boolean intersects(RegionWrapper other) {
        return other.minX <= this.maxX && other.maxX >= this.minX && other.minZ <= this.maxZ && other.maxZ >= this.minZ;
    }

    public int distance(int x, int z) {
        if (isIn(x, z)) {
            return 0;
        }
        int dx1 = Math.abs(x - minX);
        int dx2 = Math.abs(x - maxX);
        int dz1 = Math.abs(z - minZ);
        int dz2 = Math.abs(z - maxZ);
        if (x >= minX && x <= maxX) {
            return Math.min(dz1, dz2);
        } else if (z >= minZ && z <= maxZ) {
            return Math.min(dx1, dx2);
        } else {
            int dx = Math.min(dx1, dx2);
            int dz = Math.min(dz1, dz2);
            return (int) Math.sqrt(dx * dx + dz * dz);
        }
    }

    @Override
    public String toString() {
        return this.minX + "," + this.minY + "," + this.minZ + "->" + this.maxX + "," + this.maxY + "," + this.maxZ;
    }

    @Override
    public boolean isGlobal() {
        return minX == Integer.MIN_VALUE && minZ == Integer.MIN_VALUE && maxX == Integer.MAX_VALUE && maxZ == Integer.MAX_VALUE && minY <= 0 && maxY >= 255;
    }

    public boolean contains(RegionWrapper current) {
        return current.minX >= minX && current.maxX <= maxX && current.minZ >= minZ && current.maxZ <= maxZ;
    }
}
