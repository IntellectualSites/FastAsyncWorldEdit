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

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
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
    private MutableBlockVector center;

    /**
     * Stores the radii plus 0.5 on each axis.
     */
    private MutableBlockVector radius;
    private MutableBlockVector radiusSqr;
    private int radiusLengthSqr;
    private boolean sphere;

    /**
     * Construct a new instance of this ellipsoid region.
     *
     * @param pos1 the first position
     * @param pos2 the second position
     */
    public EllipsoidRegion(Vector pos1, Vector pos2) {
        this(null, pos1, pos2);
    }


    /**
     * Construct a new instance of this ellipsoid region.
     *
     * @param world  the world
     * @param center the center
     * @param radius the radius
     */
    public EllipsoidRegion(World world, Vector center, Vector radius) {
        super(world);
        this.center = new MutableBlockVector(center);
        setRadius(radius);
    }

    public EllipsoidRegion(EllipsoidRegion ellipsoidRegion) {
        this(ellipsoidRegion.world, ellipsoidRegion.center, ellipsoidRegion.getRadius());
    }

    @Override
    public Vector getMinimumPoint() {
        return center.subtract(getRadius()).clampY(0, 255);
    }

    @Override
    public Vector getMaximumPoint() {
        return center.add(getRadius()).clampY(0, 255);
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

    private Vector calculateDiff(Vector... changes) throws RegionOperationException {
        Vector diff = new Vector().add(changes);

        if ((diff.getBlockX() & 1) + (diff.getBlockY() & 1) + (diff.getBlockZ() & 1) != 0) {
            throw new RegionOperationException(
                    "Ellipsoid changes must be even for each dimensions.");
        }

        return diff.divide(2).floor();
    }

    private Vector calculateChanges(Vector... changes) {
        Vector total = new Vector();
        for (Vector change : changes) {
            total = total.add(change.positive());
        }

        return total.divide(2).floor();
    }

    @Override
    public void expand(Vector... changes) throws RegionOperationException {
        center = new MutableBlockVector(center.add(calculateDiff(changes)));
        setRadius(radius.add(calculateChanges(changes)));
    }

    @Override
    public void contract(Vector... changes) throws RegionOperationException {
        center = new MutableBlockVector(center.subtract(calculateDiff(changes)));
        Vector newRadius = radius.subtract(calculateChanges(changes));
        setRadius(Vector.getMaximum(new Vector(1.5, 1.5, 1.5), newRadius));
    }

    @Override
    public void shift(Vector change) throws RegionOperationException {
        center = new MutableBlockVector(center.add(change));
    }

    /**
     * Get the center.
     *
     * @return center
     */
    @Override
    public Vector getCenter() {
        return center;
    }

    /**
     * Set the center.
     *
     * @param center the center
     */
    public void setCenter(Vector center) {
        this.center = new MutableBlockVector(center);
    }

    /**
     * Get the radii.
     *
     * @return radii
     */
    public Vector getRadius() {
        if (radius == null) return null;
        return radius.subtract(0.5, 0.5, 0.5);
    }

    /**
     * Set the radii.
     *
     * @param radius the radius
     */
    public void setRadius(Vector radius) {
        this.radius = new MutableBlockVector(radius.add(0.5, 0.5, 0.5));
        radiusSqr = new MutableBlockVector(radius.multiply(radius));
        radiusLengthSqr = radiusSqr.getBlockX();
        if (radius.getBlockY() == radius.getBlockX() && radius.getBlockX() == radius.getBlockZ()) {
            this.sphere = true;
        } else {
            this.sphere = false;
        }
    }

    @Override
    public Set<Vector2D> getChunks() {
        final Set<Vector2D> chunks = new HashSet<Vector2D>();

        final Vector min = getMinimumPoint();
        final Vector max = getMaximumPoint();
        final int centerY = getCenter().getBlockY();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                if (!contains(new BlockVector(x, centerY, z))) {
                    continue;
                }

                chunks.add(new BlockVector2D(
                        x >> ChunkStore.CHUNK_SHIFTS,
                        z >> ChunkStore.CHUNK_SHIFTS
                ));
            }
        }

        return chunks;
    }

    @Override
    public boolean contains(Vector position) {
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
        double cxd = (double) cx / radius.getBlockX();
        double cyd = (double) cy / radius.getBlockY();
        double czd = (double) cz / radius.getBlockZ();
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

    public void extendRadius(Vector minRadius) {
        setRadius(Vector.getMaximum(minRadius, getRadius()));
    }

    @Override
    public EllipsoidRegion clone() {
        return (EllipsoidRegion) super.clone();
    }

    public static Class<EllipsoidRegion> inject() {
        return EllipsoidRegion.class;
    }
}
