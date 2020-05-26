/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;


import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an ellipsoid region.
 */
public class EllipsoidRegion extends AbstractRegion {

    /**
     * Stores the center.
     */
    private BlockVector3 center;

    /**
     * Stores the radii plus 0.5 on each axis.
     */
    private Vector3 radius;

    private Vector3 radiusSqr;
    private Vector3 inverseRadius;
    private int radiusLengthSqr;
    private boolean sphere;

    /**
     * Construct a new instance of this ellipsoid region.
     *
     * @param pos1 the first position
     * @param pos2 the second position
     */
    public EllipsoidRegion(BlockVector3 pos1, Vector3 pos2) {
        this(null, pos1, pos2);
    }

    /**
     * Construct a new instance of this ellipsoid region.
     *
     * @param world  the world
     * @param center the center
     * @param radius the radius
     */
    public EllipsoidRegion(World world, BlockVector3 center, Vector3 radius) {
        super(world);
        this.center = center;
        setRadius(radius);
    }

    public EllipsoidRegion(EllipsoidRegion ellipsoidRegion) {
        this(ellipsoidRegion.world, ellipsoidRegion.center, ellipsoidRegion.getRadius());
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return center.toVector3().subtract(getRadius()).toBlockPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return center.toVector3().add(getRadius()).toBlockPoint();
    }

    @Override
    public int getArea() {
        return (int) Math
            .floor((4.0 / 3.0) * Math.PI * radius.getX() * radius.getY() * radius.getZ());
    }

    @Override
    public int getWidth() {
        return (int) (2 * radius.getX());
    }

    @Override
    public int getHeight() {
        return (int) (2 * radius.getY());
    }

    @Override
    public int getLength() {
        return (int) (2 * radius.getZ());
    }

    private BlockVector3 calculateDiff(BlockVector3... changes) throws RegionOperationException {
        BlockVector3 diff = BlockVector3.ZERO.add(changes);

        if ((diff.getBlockX() & 1) + (diff.getBlockY() & 1) + (diff.getBlockZ() & 1) != 0) {
            throw new RegionOperationException(
                "Ellipsoid changes must be even for each dimensions.");
        }

        return diff.divide(2).floor();
    }

    private Vector3 calculateChanges(BlockVector3... changes) {
        Vector3 total = Vector3.ZERO;
        for (BlockVector3 change : changes) {
            total = total.add(change.abs().toVector3());
        }

        return total.divide(2).floor();
    }

    @Override
    public void expand(BlockVector3... changes) throws RegionOperationException {
        center = center.add(calculateDiff(changes));
        setRadius(radius.add(calculateChanges(changes)));
    }

    @Override
    public void contract(BlockVector3... changes) throws RegionOperationException {
        center = center.subtract(calculateDiff(changes));
        Vector3 newRadius = radius.subtract(calculateChanges(changes));
        setRadius(Vector3.at(1.5, 1.5, 1.5).getMaximum(newRadius));
    }

    @Override
    public void shift(BlockVector3 change) throws RegionOperationException {
        center = center.add(change);
    }

    /**
     * Get the center.
     *
     * @return center
     */
    @Override
    public Vector3 getCenter() {
        return center.toVector3();
    }

    /**
     * Set the center.
     *
     * @param center the center
     */
    public void setCenter(BlockVector3 center) {
        this.center = center;
    }

    /**
     * Get the radii.
     *
     * @return radii
     */
    public Vector3 getRadius() {
        return radius.subtract(0.5, 0.5, 0.5);
    }

    /**
     * Set the radii.
     *
     * @param radius the radius
     */
    public void setRadius(Vector3 radius) {
        this.radius = radius.add(0.5, 0.5, 0.5);
        radiusSqr = radius.multiply(radius);
        radiusLengthSqr = (int) radiusSqr.getX();
        this.sphere = radius.getY() == radius.getX() && radius.getX() == radius.getZ();
        inverseRadius = Vector3.ONE.divide(radius);
    }

    @Override
    public Set<BlockVector2> getChunks() {
        final Set<BlockVector2> chunks = new HashSet<>();

        final BlockVector3 min = getMinimumPoint();
        final BlockVector3 max = getMaximumPoint();
        final int centerY = center.getBlockY();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                if (!contains(x, centerY, z)) {
                    continue;
                }

                chunks.add(
                    BlockVector2.at(x >> ChunkStore.CHUNK_SHIFTS, z >> ChunkStore.CHUNK_SHIFTS));
            }
        }

        return chunks;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        int cx = x - center.getBlockX();
        int cx2 = cx * cx;
        if (cx2 > radiusSqr.getBlockX()) {
            return false;
        }
        int cz = z - center.getBlockZ();
        int cz2 = cz * cz;
        if (cz2 > radiusSqr.getBlockZ()) {
            return false;
        }
        int cy = y - center.getBlockY();
        int cy2 = cy * cy;
        if (radiusSqr.getBlockY() < 255 && cy2 > radiusSqr.getBlockY()) {
            return false;
        }
        if (sphere) {
            return cx2 + cy2 + cz2 <= radiusLengthSqr;
        }
        double cxd = cx2 * inverseRadius.getX();
        double cyd = cy2 * inverseRadius.getY();
        double czd = cz2 * inverseRadius.getZ();
        return cxd + cyd + czd <= 1;
    }

    /*
    /* Slow and unnecessary
    @Override
    public boolean contains(BlockVector3 position) {
        return position.subtract(center).toVector3().divide(radius).lengthSq() <= 1;
    }
    */

    @Override
    public boolean contains(BlockVector3 position) {
        return contains(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean contains(int x, int z) {
        int cx = x - center.getBlockX();
        int cx2 = cx * cx;
        if (cx2 > radiusSqr.getBlockX()) {
            return false;
        }
        int cz = z - center.getBlockZ();
        int cz2 = cz * cz;
        if (cz2 > radiusSqr.getBlockZ()) {
            return false;
        }
        double cxd = cx2 * inverseRadius.getX();
        double czd = cz2 * inverseRadius.getZ();
        return cxd + czd <= 1;
    }

    /**
     * Returns string representation in the format
     * "(centerX, centerY, centerZ) - (radiusX, radiusY, radiusZ)".
     *
     * @return string
     */
    @Override
    public String toString() {
        return center + " - " + getRadius();
    }

    public void extendRadius(Vector3 minRadius) {
        setRadius(minRadius.getMaximum(getRadius()));
    }

    @Override
    public EllipsoidRegion clone() {
        return (EllipsoidRegion) super.clone();
    }

    private void filterSpherePartial(int y1, int y2, int bx, int bz, Filter filter,
        ChunkFilterBlock block, IChunkGet get, IChunkSet set) {
        int minSection = y1 >> 4;
        int maxSection = y2 >> 4;
        int yStart = (y1 & 15);
        int yEnd = (y2 & 15);

        if (minSection == maxSection) {
            filterSpherePartial(minSection, 0, 15, bx, bz, filter, block, get, set);
        }

        if (yStart != 0) {
            filterSpherePartial(minSection, yStart, 15, bx, bz, filter, block, get, set);
            minSection++;
        }

        if (yEnd != 15) {
            filterSpherePartial(maxSection, 0, yEnd, bx, bz, filter, block, get, set);
            maxSection--;
        }

        for (int layer = minSection; layer <= maxSection; layer++) {
            filterSpherePartial(layer, 0, 15, bx, bz, filter, block, get, set);
        }
    }

    private void filterSpherePartial(int layer, int y1, int y2, int bx, int bz, Filter filter,
        ChunkFilterBlock block, IChunkGet get, IChunkSet set) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        block.initLayer(get, set, layer);

        int by = layer << 4;
        int diffY;
        for (int y = y1, yy = by + y1; y <= y2; y++, yy++) {
            diffY = cy - yy;
            int remainderY = radiusLengthSqr - (diffY * diffY);
            if (remainderY < 0) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                int diffZ = cz - zz;
                int remainderZ = remainderY - (diffZ * diffZ);
                if (remainderZ < 0) {
                    continue;
                }
                int diffX, minX, maxX;
                diffX = (int) Math.floor(Math.sqrt(remainderZ));
                minX = Math.max(0, cx - diffX - bx);
                maxX = Math.min(15, cx + diffX - bx);
                block.filter(filter, minX, y, z, maxX, y, z);
            }
        }

    }

    @Override
    public void filter(IChunk chunk, Filter filter, ChunkFilterBlock block, IChunkGet get,
        IChunkSet set, boolean full) {
        // Check bounds
        // This needs to be able to perform 50M blocks/sec otherwise it becomes a bottleneck
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        int cx1 = Math.abs(bx - cx);
        int cx2 = Math.abs(tx - cx);
        int cxMax, cxMin;
        cxMin = Math.min(cx1, cx2);
        cxMax = Math.max(cx1, cx2);
        int cxMin2 = cxMin * cxMin;
        int cxMax2 = cxMax * cxMax;

        int cz1 = Math.abs(bz - cz);
        int cz2 = Math.abs(tz - cz);
        int czMax, czMin;
        czMin = Math.min(cz1, cz2);
        czMax = Math.max(cz1, cz2);
        int czMin2 = czMin * czMin;
        int czMax2 = czMax * czMax;

        if (sphere) {
            // Does not contain whole chunk
            if (cxMin2 + czMin2 >= radiusLengthSqr) {
                super.filter(chunk, filter, block, get, set, full);
                return;
            }

            int diffY2 = radiusLengthSqr - cxMax2 - czMax2;

            // Does not contain whole chunk
            if (diffY2 < 0) {
                super.filter(chunk, filter, block, get, set, full);
                return;
            }

            block = block.initChunk(chunk.getX(), chunk.getZ());

            // Get the solid layers
            int cy = center.getBlockY();
            int diffYFull = MathMan.usqrt(diffY2);

            int yBotFull = Math.max(0, cy - diffYFull);
            int yTopFull = Math.min(255, cy + diffYFull);

            if (yBotFull == yTopFull || yBotFull > yTopFull) {
                System.out.println("aa");
            }
            // Set those layers
            filter(chunk, filter, block, get, set, yBotFull, yTopFull, full);

            if (yBotFull == 0 && yTopFull == 255) {
                return;
            }

            int diffYPartial = MathMan.usqrt(radiusLengthSqr - cxMin * cxMin - czMin * czMin);

            //Fill the remaining layers
            if (yBotFull != 0) {
                int yBotPartial = Math.max(0, cy - diffYPartial);
                filterSpherePartial(yBotPartial, yBotFull - 1, bx, bz, filter, block, get, set);
            }

            if (yTopFull != 255) {
                int yTopPartial = Math.min(255, cy + diffYPartial);
                filterSpherePartial(yTopFull + 1, yTopPartial, bx, bz, filter, block, get, set);
            }

        } else {
            super.filter(chunk, filter, block, get, set, full);
        }
    }
}
