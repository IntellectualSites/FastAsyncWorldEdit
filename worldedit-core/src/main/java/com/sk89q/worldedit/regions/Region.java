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

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Represents a physical shape.
 */
public interface Region extends Iterable<BlockVector>, Cloneable {

    /**
     * Get the lower point of a region.
     *
     * @return min. point
     */
    public Vector getMinimumPoint();

    /**
     * Get the upper point of a region.
     *
     * @return max. point
     */
    public Vector getMaximumPoint();

    /**
     * Get the center point of a region.
     * Note: Coordinates will not be integers
     * if the corresponding lengths are even.
     *
     * @return center point
     */
    public Vector getCenter();

    /**
     * Get the number of blocks in the region.
     *
     * @return number of blocks
     */
    public int getArea();

    /**
     * Get X-size.
     *
     * @return width
     */
    public int getWidth();

    /**
     * Get Y-size.
     *
     * @return height
     */
    public int getHeight();

    /**
     * Get Z-size.
     *
     * @return length
     */
    public int getLength();

    /**
     * Expand the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    public void expand(Vector... changes) throws RegionOperationException;

    /**
     * Contract the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    public void contract(Vector... changes) throws RegionOperationException;

    /**
     * Shift the region.
     *
     * @param change the change
     * @throws RegionOperationException
     */
    public void shift(Vector change) throws RegionOperationException;

    /**
     * Returns true based on whether the region contains the point.
     *
     * @param position the position
     * @return true if contained
     */
    public boolean contains(Vector position);

    default boolean contains(int x, int y, int z) {
        return contains(MutableBlockVector.get(x, y, z));
    }

    default boolean contains(int x, int z) {
        return contains(MutableBlockVector.get(x, 0, z));
    }

    default boolean isGlobal() {
        Vector pos1 = getMinimumPoint();
        Vector pos2 = getMaximumPoint();
        return pos1.getBlockX() == Integer.MIN_VALUE && pos1.getBlockZ() == Integer.MIN_VALUE && pos2.getBlockX() == Integer.MAX_VALUE && pos2.getBlockZ() == Integer.MAX_VALUE && pos1.getBlockY() <= 0 && pos2.getBlockY() >= 255;
    }

    /**
     * Get a list of chunks.
     *
     * @return a list of chunk coordinates
     */
    public Set<Vector2D> getChunks();

    /**
     * Return a list of 16*16*16 chunks in a region
     *
     * @return the chunk cubes this region overlaps with
     */
    public Set<Vector> getChunkCubes();

    /**
     * Sets the world that the selection is in.
     *
     * @return the world, or null
     */
    @Nullable
    public World getWorld();

    /**
     * Sets the world that the selection is in.
     *
     * @param world the world, which may be null
     */
    public void setWorld(@Nullable World world);

    /**
     * Make a clone of the region.
     *
     * @return a cloned version
     */
    public Region clone();

    /**
     * Polygonizes a cross-section or a 2D projection of the region orthogonal to the Y axis.
     *
     * @param maxPoints maximum number of points to generate. -1 for no limit.
     * @return the points.
     */
    public List<BlockVector2D> polygonize(int maxPoints);
}
