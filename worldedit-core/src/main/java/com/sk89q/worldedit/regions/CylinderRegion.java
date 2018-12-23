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

<<<<<<< HEAD
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
=======
import static com.google.common.base.Preconditions.checkNotNull;

>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.geom.Polygons;
import com.sk89q.worldedit.regions.iterator.FlatRegion3DIterator;
import com.sk89q.worldedit.regions.iterator.FlatRegionIterator;
import com.sk89q.worldedit.world.World;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a cylindrical region.
 */
public class CylinderRegion extends AbstractRegion implements FlatRegion {

<<<<<<< HEAD
    private BlockVector2D center;
    private BlockVector2D radius;
    private Vector2D radiusInverse;
=======
    private BlockVector2 center;
    private Vector2 radius;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    private int minY;
    private int maxY;
    private boolean hasY = false;

    /**
     * Construct the region
     */
    public CylinderRegion() {
        this((World) null);
    }

    /**
     * Construct the region.
     *
     * @param world the world
     */
    public CylinderRegion(World world) {
        this(world, BlockVector3.ZERO, Vector2.ZERO, 0, 0);
        hasY = false;
    }

    /**
     * Construct the region.
     *
     * @param world the world
     * @param center the center position
     * @param radius the radius along the X and Z axes
     * @param minY the minimum Y, inclusive
     * @param maxY the maximum Y, inclusive
     */
    public CylinderRegion(World world, BlockVector3 center, Vector2 radius, int minY, int maxY) {
        super(world);
        setCenter(center.toBlockVector2());
        setRadius(radius);
        this.minY = minY;
        this.maxY = maxY;
        hasY = true;
    }

    /**
     * Construct the region.
     *
     * @param center the center position
     * @param radius the radius along the X and Z axes
     * @param minY the minimum Y, inclusive
     * @param maxY the maximum Y, inclusive
     */
    public CylinderRegion(BlockVector3 center, Vector2 radius, int minY, int maxY) {
        super(null);
        setCenter(center.toBlockVector2());
        setRadius(radius);
        this.minY = minY;
        this.maxY = maxY;
        hasY = true;
    }

    public CylinderRegion(CylinderRegion region) {
        this(region.world, region.getCenter().toBlockPoint(), region.getRadius(), region.minY, region.maxY);
        hasY = region.hasY;
    }

    @Override
<<<<<<< HEAD
    public Vector getCenter() {
        return center.toVector((getMaximumY() + getMinimumY()) / 2);
    }

    /**
     * Sets the main center point of the region
     *
     * @deprecated replaced by {@link #setCenter(Vector2D)}
     */
    @Deprecated
    public void setCenter(Vector center) {
        setCenter(center.toVector2D());
=======
    public Vector3 getCenter() {
        return center.toVector3((maxY + minY) / 2);
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    /**
     * Sets the main center point of the region
     *
     * @param center the center point
     */
<<<<<<< HEAD
    public void setCenter(Vector2D center) {
        this.center = new BlockVector2D(center);
=======
    public void setCenter(BlockVector2 center) {
        this.center = center;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    /**
     * Returns the radius of the cylinder
     *
     * @return the radius along the X and Z axes
     */
    public Vector2 getRadius() {
        return radius.subtract(0.5, 0.5);
    }

    /**
     * Sets the radius of the cylinder
     *
     * @param radius the radius along the X and Z axes
     */
<<<<<<< HEAD
    public void setRadius(Vector2D radius) {
        this.radius = radius.add(0.5, 0.5).toBlockVector2D();
        this.radiusInverse = Vector2D.ONE.divide(radius);
=======
    public void setRadius(Vector2 radius) {
        this.radius = radius.add(0.5, 0.5);
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    /**
     * Extends the radius to be at least the given radius
     *
     * @param minRadius the minimum radius
     */
    public void extendRadius(Vector2 minRadius) {
        setRadius(minRadius.getMaximum(getRadius()));
    }

    /**
     * Set the minimum Y.
     *
     * @param y the y
     */
    public void setMinimumY(int y) {
        hasY = true;
        minY = y;
    }

    /**
     * Se the maximum Y.
     *
     * @param y the y
     */
    public void setMaximumY(int y) {
        hasY = true;
        maxY = y;
    }

    @Override
<<<<<<< HEAD
    public Vector getMinimumPoint() {
        return center.subtract(getRadius()).toVector(getMinimumY());
    }

    @Override
    public Vector getMaximumPoint() {
        return center.add(getRadius()).toVector(getMaximumY());
=======
    public BlockVector3 getMinimumPoint() {
        return center.toVector2().subtract(getRadius()).toVector3(minY).toBlockPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return center.toVector2().add(getRadius()).toVector3(maxY).toBlockPoint();
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    @Override
    public int getMaximumY() {
        int worldMax = world != null ? world.getMaxY() - 1 : 255;
        if (maxY > worldMax) {
            return maxY = worldMax;
        }
        return maxY;
    }

    @Override
    public int getMinimumY() {
        if (minY < 0) {
            return minY = 0;
        }
        return minY;
    }

    @Override
    public int getArea() {
        return (int) Math.floor(radius.getX() * radius.getZ() * Math.PI * getHeight());
    }

    @Override
    public int getWidth() {
        return (int) (2 * radius.getX());
    }

    @Override
    public int getHeight() {
        return getMaximumY() - getMinimumY() + 1;
    }

    @Override
    public int getLength() {
        return (int) (2 * radius.getZ());
    }

    private BlockVector2 calculateDiff2D(BlockVector3... changes) throws RegionOperationException {
        BlockVector2 diff = BlockVector2.ZERO;
        for (BlockVector3 change : changes) {
            diff = diff.add(change.toBlockVector2());
        }

        if ((diff.getBlockX() & 1) + (diff.getBlockZ() & 1) != 0) {
            throw new RegionOperationException("Cylinders changes must be even for each horizontal dimensions.");
        }

        return diff.divide(2).floor();
    }

    private BlockVector2 calculateChanges2D(BlockVector3... changes) {
        BlockVector2 total = BlockVector2.ZERO;
        for (BlockVector3 change : changes) {
            total = total.add(change.toBlockVector2().abs());
        }

        return total.divide(2).floor();
    }

    /**
     * Expand the region.
     * Expand the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    @Override
<<<<<<< HEAD
    public void expand(Vector... changes) throws RegionOperationException {
        center = center.add(calculateDiff2D(changes)).toBlockVector2D();
        radius = radius.add(calculateChanges2D(changes)).toBlockVector2D();
        this.radiusInverse = Vector2D.ONE.divide(radius);
        for (Vector change : changes) {
=======
    public void expand(BlockVector3... changes) throws RegionOperationException {
        center = center.add(calculateDiff2D(changes));
        radius = radius.add(calculateChanges2D(changes).toVector2());
        for (BlockVector3 change : changes) {
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
            int changeY = change.getBlockY();
            if (changeY > 0) {
                maxY += changeY;
            } else {
                minY += changeY;
            }
        }
    }

    /**
     * Contract the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    @Override
<<<<<<< HEAD
    public void contract(Vector... changes) throws RegionOperationException {
        center = center.subtract(calculateDiff2D(changes)).toBlockVector2D();
        Vector2D newRadius = radius.subtract(calculateChanges2D(changes));
        radius = Vector2D.getMaximum(new Vector2D(1.5, 1.5), newRadius).toBlockVector2D();
        this.radiusInverse = Vector2D.ONE.divide(radius);
        for (Vector change : changes) {
=======
    public void contract(BlockVector3... changes) throws RegionOperationException {
        center = center.subtract(calculateDiff2D(changes));
        Vector2 newRadius = radius.subtract(calculateChanges2D(changes).toVector2());
        radius = new Vector2(1.5, 1.5).getMaximum(newRadius);
        for (BlockVector3 change : changes) {
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
            int height = maxY - minY;
            int changeY = change.getBlockY();
            if (changeY > 0) {
                minY += Math.min(height, changeY);
            } else {
                maxY += Math.max(-height, changeY);
            }
        }
    }

    @Override
<<<<<<< HEAD
    public void shift(Vector change) throws RegionOperationException {
        center = center.add(change.toVector2D()).toBlockVector2D();
=======
    public void shift(BlockVector3 change) throws RegionOperationException {
        center = center.add(change.toBlockVector2());
>>>>>>> 399e0ad5... Refactor vector system to be cleaner

        int changeY = change.getBlockY();
        maxY += changeY;
        minY += changeY;
    }

    /**
     * Checks to see if a point is inside this region.
     */
    @Override
<<<<<<< HEAD
    public boolean contains(Vector position) {
        final int pt = position.getBlockY();
        if (pt < getMinimumY() || pt > getMaximumY()) {
=======
    public boolean contains(BlockVector3 position) {
        final int blockY = position.getBlockY();
        if (blockY < minY || blockY > maxY) {
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
            return false;
        }
        int px = position.getBlockX();
        int pz = position.getBlockZ();

        double dx = Math.abs(px - center.getBlockX()) * radiusInverse.getX();
        double dz = Math.abs(pz - center.getBlockZ()) * radiusInverse.getZ();

<<<<<<< HEAD
        return dx * dx + dz * dz <= 1;
=======
        return position.toBlockVector2().subtract(center).toVector2().divide(radius).lengthSq() <= 1;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }


    /**
     * Sets the height of the cylinder to fit the specified Y.
     *
     * @param y the y value
     * @return true if the area was expanded
     */
    public boolean setY(int y) {
        if (!hasY) {
            minY = y;
            maxY = y;
            hasY = true;
            return true;
        } else if (y < minY) {
            minY = y;
            return true;
        } else if (y > maxY) {
            maxY = y;
            return true;
        }

        return false;
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        return new FlatRegion3DIterator(this);
    }

    @Override
<<<<<<< HEAD
    public Iterable<Vector2D> asFlatRegion() {
        return new Iterable<Vector2D>() {
            @Override
            public Iterator<Vector2D> iterator() {
                return new FlatRegionIterator(CylinderRegion.this);
            }
        };
=======
    public Iterable<BlockVector2> asFlatRegion() {
        return () -> new FlatRegionIterator(CylinderRegion.this);
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    /**
     * Returns string representation in the format
     * "(centerX, centerZ) - (radiusX, radiusZ) - (minY, maxY)"
     *
     * @return string
     */
    @Override
    public String toString() {
        return center + " - " + radius + "(" + minY + ", " + maxY + ")";
    }

    @Override
    public CylinderRegion clone() {
        return (CylinderRegion) super.clone();
    }

    @Override
    public List<BlockVector2> polygonize(int maxPoints) {
        return Polygons.polygonizeCylinder(center, radius, maxPoints);
    }

    /**
     * Return a new instance with the given center and radius in the X and Z
     * axes with a Y that extends from the bottom of the extent to the top
     * of the extent.
     *
     * @param extent the extent
     * @param center the center position
     * @param radius the radius in the X and Z axes
     * @return a region
     */
    public static CylinderRegion createRadius(Extent extent, BlockVector3 center, double radius) {
        checkNotNull(extent);
        checkNotNull(center);
        Vector2 radiusVec = new Vector2(radius, radius);
        int minY = extent.getMinimumPoint().getBlockY();
        int maxY = extent.getMaximumPoint().getBlockY();
        return new CylinderRegion(center, radiusVec, minY, maxY);
    }
}
