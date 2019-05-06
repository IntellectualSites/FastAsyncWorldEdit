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


import com.sk89q.worldedit.math.MutableBlockVector3;
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
        if (radius == null) return 0;
        return (int) Math.floor((4.0 / 3.0) * Math.PI * radius.getX() * radius.getY() * radius.getZ());
    }

    @Override
    public int getWidth() {
        return (int) (2 * radius.getX());
    }

    @Override
    public int getHeight() {
        return Math.max((int) (2 * radius.getY()), 256);
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
        if (radius == null) return null;
        return radius.subtract(0.5, 0.5, 0.5);
    }

    /**
     * Set the radii.
     *
     * @param radius the radius
     */
    public void setRadius(Vector3 radius) {
        this.radius = radius;
        radiusSqr = radius.multiply(radius);
        radiusLengthSqr = (int) radiusSqr.getX();
        if (radius.getY() == radius.getX() && radius.getX() == radius.getZ()) {
            this.sphere = true;
        } else {
            this.sphere = false;
        }
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
                if (!contains(BlockVector3.at(x, centerY, z))) {
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

    @Override
   public boolean contains(BlockVector3 position) {
       int cx = position.getBlockX() - center.getBlockX();
       int cx2 = cx * cx;
       if (cx2 > radiusSqr.getBlockX()) {
           return false;
       }
       int cz = position.getBlockZ() - center.getBlockZ();
       int cz2 = cz * cz;
       if (cz2 > radiusSqr.getBlockZ()) {
           return false;
       }
       int cy = position.getBlockY() - center.getBlockY();
       int cy2 = cy * cy;
       if (radiusSqr.getBlockY() < 255 && cy2 > radiusSqr.getBlockY()) {
           return false;
       }
       if (sphere) {
           return cx2 + cy2 + cz2 <= radiusLengthSqr;
       }
       double cxd = cx * inverseRadius.getX();
       double cyd = cy * inverseRadius.getY();
       double czd = cz * inverseRadius.getZ();
       return cxd * cxd + cyd * cyd + czd * czd <= 1;
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
}
