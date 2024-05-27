/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;


import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    //FAWE start
    private Vector3 radiusSqr;
    private Vector3 inverseRadiusSqr;
    private int radiusLengthSqr;
    private boolean sphere;
    //FAWE end

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

    private static final BigDecimal ELLIPSOID_BASE_MULTIPLIER = BigDecimal.valueOf((4.0 / 3.0) * Math.PI);

    @Override
    public long getVolume() {
        return ELLIPSOID_BASE_MULTIPLIER
                .multiply(BigDecimal.valueOf(radius.x()))
                .multiply(BigDecimal.valueOf(radius.y()))
                .multiply(BigDecimal.valueOf(radius.z()))
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
    }

    @Override
    public int getWidth() {
        return (int) (2 * radius.x());
    }

    @Override
    public int getHeight() {
        return (int) (2 * radius.y());
    }

    @Override
    public int getLength() {
        return (int) (2 * radius.z());
    }

    private BlockVector3 calculateDiff(BlockVector3... changes) throws RegionOperationException {
        BlockVector3 diff = BlockVector3.ZERO.add(changes);

        if ((diff.x() & 1) + (diff.y() & 1) + (diff.z() & 1) != 0) {
            throw new RegionOperationException(Caption.of("worldedit.selection.ellipsoid.error.even-horizontal"));
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
        //FAWE start
        radiusSqr = radius.multiply(radius);
        radiusLengthSqr = (int) radiusSqr.x();
        this.sphere = radius.y() == radius.x() && radius.x() == radius.z();
        inverseRadiusSqr = Vector3.ONE.divide(radiusSqr);
        //FAWE end
    }

    @Override
    public Set<BlockVector2> getChunks() {
        final Set<BlockVector2> chunks = new HashSet<>();

        final BlockVector3 min = getMinimumPoint();
        final BlockVector3 max = getMaximumPoint();
        final int centerY = center.y();

        for (int x = min.x(); x <= max.x(); ++x) {
            for (int z = min.z(); z <= max.z(); ++z) {
                if (!contains(x, centerY, z)) {
                    continue;
                }

                chunks.add(BlockVector2.at(
                        x >> ChunkStore.CHUNK_SHIFTS,
                        z >> ChunkStore.CHUNK_SHIFTS
                ));
            }
        }

        return chunks;
    }

    //FAWE start
    @Override
    public boolean contains(int x, int y, int z) {
        int cx = x - center.x();
        int cx2 = cx * cx;
        if (cx2 > radiusSqr.getBlockX()) {
            return false;
        }
        int cz = z - center.z();
        int cz2 = cz * cz;
        if (cz2 > radiusSqr.getBlockZ()) {
            return false;
        }
        int cy = y - center.y();
        int cy2 = cy * cy;
        if (radiusSqr.getBlockY() < getWorldMaxY() && cy2 > radiusSqr.getBlockY()) {
            return false;
        }
        if (sphere) {
            return cx2 + cy2 + cz2 <= radiusLengthSqr;
        }
        double cxd = cx2 * inverseRadiusSqr.x();
        double cyd = cy2 * inverseRadiusSqr.y();
        double czd = cz2 * inverseRadiusSqr.z();
        return cxd + cyd + czd <= 1;
    }

    /* FAWE start
    /* Slow and unnecessary
    @Override
    public boolean contains(BlockVector3 position) {
        return position.subtract(center).toVector3().divide(radius).lengthSq() <= 1;
    }
     */

    @Override
    public boolean contains(BlockVector3 position) {
        return contains(position.x(), position.y(), position.z());
    }

    @Override
    public boolean contains(int x, int z) {
        int cx = x - center.x();
        int cx2 = cx * cx;
        if (cx2 > radiusSqr.getBlockX()) {
            return false;
        }
        int cz = z - center.z();
        int cz2 = cz * cz;
        if (cz2 > radiusSqr.getBlockZ()) {
            return false;
        }
        double cxd = cx2 * inverseRadiusSqr.x();
        double czd = cz2 * inverseRadiusSqr.z();
        return cxd + czd <= 1;
    }
    //FAWE end

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

    //FAWE start
    private void filterSpherePartial(
            int y1, int y2, int bx, int bz, Filter filter,
            ChunkFilterBlock block, IChunkGet get, IChunkSet set
    ) {
        int minSection = y1 >> 4;
        int maxSection = y2 >> 4;
        int yStart = (y1 & 15);
        int yEnd = (y2 & 15);

        if (minSection == maxSection) {
            filterSpherePartial(minSection, 0, 15, bx, bz, filter, block, get, set);
        }

        if (yStart != getWorldMinY()) {
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

    private void filterSpherePartial(
            int layer, int y1, int y2, int bx, int bz, Filter filter,
            ChunkFilterBlock block, IChunkGet get, IChunkSet set
    ) {
        int cx = center.x();
        int cy = center.y();
        int cz = center.z();

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
                int diffX = (int) Math.floor(Math.sqrt(remainderZ));
                int minX = Math.max(0, cx - diffX - bx);
                int maxX = Math.min(15, cx + diffX - bx);
                block.filter(filter, minX, y, z, maxX, y, z);
            }
        }

    }

    @Override
    public void filter(
            IChunk chunk, Filter filter, ChunkFilterBlock block, IChunkGet get,
            IChunkSet set, boolean full
    ) {
        // Check bounds
        // This needs to be able to perform 50M blocks/sec otherwise it becomes a bottleneck
        int cx = center.x();
        int cz = center.z();
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        int cx1 = Math.abs(bx - cx);
        int cx2 = Math.abs(tx - cx);
        int cxMin = Math.min(cx1, cx2);
        int cxMax = Math.max(cx1, cx2);
        int cxMin2 = cxMin * cxMin;
        int cxMax2 = cxMax * cxMax;

        int cz1 = Math.abs(bz - cz);
        int cz2 = Math.abs(tz - cz);
        int czMin = Math.min(cz1, cz2);
        int czMax = Math.max(cz1, cz2);
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
            int cy = center.y();
            int diffYFull = MathMan.usqrt(diffY2);

            int yBotFull = Math.max(getWorldMinY(), cy - diffYFull);
            int yTopFull = Math.min(getWorldMaxY(), cy + diffYFull);

            if (yBotFull == yTopFull || yBotFull > yTopFull) {
            }
            // Set those layers
            filter(chunk, filter, block, get, set, yBotFull, yTopFull, full);

            if (yBotFull == getWorldMinY() && yTopFull == getWorldMaxY()) {
                return;
            }

            int diffYPartial = MathMan.usqrt(radiusLengthSqr - cxMin * cxMin - czMin * czMin);

            //Fill the remaining layers
            if (yBotFull != getWorldMinY()) {
                int yBotPartial = Math.max(getWorldMinY(), cy - diffYPartial);
                filterSpherePartial(yBotPartial, yBotFull - 1, bx, bz, filter, block, get, set);
            }

            if (yTopFull != getWorldMaxY()) {
                int yTopPartial = Math.min(getWorldMaxY(), cy + diffYPartial);
                filterSpherePartial(yTopFull + 1, yTopPartial, bx, bz, filter, block, get, set);
            }

        } else {
            super.filter(chunk, filter, block, get, set, full);
        }
    }
    //FAWE end
}
