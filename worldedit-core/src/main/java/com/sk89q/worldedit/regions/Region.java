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

import com.boydti.fawe.beta.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a physical shape.
 */
public interface Region extends Iterable<BlockVector3>, Cloneable {

    /**
     * Get the lower point of a region.
     *
     * @return min. point
     */
    BlockVector3 getMinimumPoint();

    /**
     * Get the upper point of a region.
     *
     * @return max. point
     */
    BlockVector3 getMaximumPoint();

    /**
     * Get the center point of a region.
     * Note: Coordinates will not be integers
     * if the corresponding lengths are even.
     *
     * @return center point
     */
    Vector3 getCenter();

    /**
     * Get the number of blocks in the region.
     *
     * @return number of blocks
     */
    int getArea();

    /**
     * Get X-size.
     *
     * @return width
     */
    int getWidth();

    /**
     * Get Y-size.
     *
     * @return height
     */
    int getHeight();

    /**
     * Get Z-size.
     *
     * @return length
     */
    int getLength();

    /**
     * Expand the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    void expand(BlockVector3... changes) throws RegionOperationException;

    /**
     * Contract the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException
     */
    void contract(BlockVector3... changes) throws RegionOperationException;

    /**
     * Shift the region.
     *
     * @param change the change
     * @throws RegionOperationException
     */
    void shift(BlockVector3 change) throws RegionOperationException;

    default boolean contains(int x, int y, int z) {
        return contains(BlockVector3.at(x, y, z));
    }

    default boolean contains(int x, int z) {
        return contains(BlockVector3.at(x, 0, z));
    }

    default boolean isGlobal() {
        BlockVector3 pos1 = getMinimumPoint();
        BlockVector3 pos2 = getMaximumPoint();
        return pos1.getBlockX() == Integer.MIN_VALUE && pos1.getBlockZ() == Integer.MIN_VALUE && pos2.getBlockX() == Integer.MAX_VALUE && pos2.getBlockZ() == Integer.MAX_VALUE && pos1.getBlockY() <= 0 && pos2.getBlockY() >= 255;
    }

    /**
     * Returns true based on whether the region contains the point.
     *
     * @param position the position
     * @return true if contained
     */
    default boolean contains(BlockVector3 position) {
        return contains(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Get a list of chunks.
     *
     * @return a list of chunk coordinates
     */
    Set<BlockVector2> getChunks();

    /**
     * Return a list of 16*16*16 chunks in a region
     *
     * @return the chunk cubes this region overlaps with
     */
    Set<BlockVector3> getChunkCubes();

    /**
     * Sets the world that the selection is in.
     *
     * @return the world, or null
     */
    @Nullable World getWorld();

    /**
     * Sets the world that the selection is in.
     *
     * @param world the world, which may be null
     */
    void setWorld(@Nullable World world);

    /**
     * Make a clone of the region.
     *
     * @return a cloned version
     */
    Region clone();

    /**
     * Polygonizes a cross-section or a 2D projection of the region orthogonal to the Y axis.
     *
     * @param maxPoints maximum number of points to generate. -1 for no limit.
     * @return the points.
     */
    List<BlockVector2> polygonize(int maxPoints);

    default int getMinY() {
        return getMinimumPoint().getY();
    }

    default int getMaxY() {
        return getMaximumPoint().getY();
    }

    default void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set) {
        int minSection = Math.max(0, getMinY() >> 4);
        int maxSection = Math.min(15, getMaxY() >> 4);
        for (int layer = minSection; layer <= maxSection; layer++) {
            if (!get.hasSection(layer) || !filter.appliesLayer(chunk, layer)) return;
            block = block.init(get, set, layer);
            block.filter(filter, this);
        }
    }

    default void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set, final int minY, final int maxY) {
        int minSection = minY >> 4;
        int maxSection = maxY >> 4;
        int yStart = (minY & 15);
        int yEnd = (maxY & 15);
        if (minSection == maxSection) {
            filter(chunk, filter, block, get, set, minSection, yStart, yEnd);
            return;
        }
        if (yStart != 0) {
            filter(chunk, filter, block, get, set, minSection, yStart, 15);
            minSection++;
        }
        if (yEnd != 15) {
            filter(chunk, filter, block, get, set, minSection, 0, yEnd);
            maxSection--;
        }
        for (int layer = minSection; layer < maxSection; layer++) {
            filter(chunk, filter, block, get, set, layer);
        }
        return;
    }

    default void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set, int layer) {
        if (!get.hasSection(layer) || !filter.appliesLayer(chunk, layer)) return;
        block = block.init(get, set, layer);
        block.filter(filter);
    }

    default void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set, int layer, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!get.hasSection(layer) || !filter.appliesLayer(chunk, layer)) return;
        block = block.init(get, set, layer);
        block.filter(filter, minX, minY, minZ, maxX, maxY, maxZ);
    }

    default void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set, int layer, int yStart, int yEnd) {
        if (!get.hasSection(layer) || !filter.appliesLayer(chunk, layer)) return;
        block = block.init(get, set, layer);
        block.filter(filter, yStart, yEnd);
    }
}
