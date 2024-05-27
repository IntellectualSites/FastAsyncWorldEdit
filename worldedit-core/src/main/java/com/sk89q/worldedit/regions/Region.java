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

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.SingleRegionExtent;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.DeprecationUtil;
import com.sk89q.worldedit.internal.util.NonAbstractForCompatibility;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Represents a physical shape.
 */
//FAWE start - IBatchProcessor
public interface Region extends Iterable<BlockVector3>, Cloneable, IBatchProcessor {
//FAWE end

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

    //FAWE start
    default BlockVector3 getDimensions() {
        return getMaximumPoint().subtract(getMinimumPoint()).add(1, 1, 1);
    }
    //FAWE end

    /**
     * Get the bounding box of this region as a {@link CuboidRegion}.
     *
     * @return the bounding box
     */
    default CuboidRegion getBoundingBox() {
        return new CuboidRegion(getMinimumPoint(), getMaximumPoint());
    }

    /**
     * Get the center point of a region.
     * Note: Coordinates will not be integers
     * if the corresponding lengths are even.
     *
     * @return center point
     */
    default Vector3 getCenter() {
        return getMinimumPoint().add(getMaximumPoint()).toVector3().divide(2);
    }

    /**
     * Get the number of blocks in the region.
     *
     * @return number of blocks
     * @deprecated use {@link Region#getVolume()} to prevent overflows
     */
    @Deprecated
    default int getArea() {
        return (int) getVolume();
    }

    /**
     * Get the number of blocks in the region.
     *
     * @return number of blocks
     */
    @NonAbstractForCompatibility(
            delegateName = "getArea",
            delegateParams = {}
    )
    default long getVolume() {
        DeprecationUtil.checkDelegatingOverride(getClass());

        return getArea();
    }

    /* FAWE code for getArea() before merge:
        default int getArea() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }
    */

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
     * @throws RegionOperationException if the operation cannot be performed
     */
    void expand(BlockVector3... changes) throws RegionOperationException;

    /**
     * Contract the region.
     *
     * @param changes array/arguments with multiple related changes
     * @throws RegionOperationException if the operation cannot be performed
     */
    void contract(BlockVector3... changes) throws RegionOperationException;

    /**
     * Shift the region.
     *
     * @param change the change
     * @throws RegionOperationException if the operation cannot be performed
     */
    void shift(BlockVector3 change) throws RegionOperationException;

    /**
     * Returns true based on whether the region contains the point.
     *
     * @param position the position
     * @return true if contained
     */
    boolean contains(BlockVector3 position);

    /**
     * Get a list of chunks.
     *
     * @return a list of chunk coordinates
     */
    Set<BlockVector2> getChunks();

    /**
     * Return a list of 16*16*16 chunks in a region.
     *
     * @return the chunk cubes this region overlaps with
     */
    Set<BlockVector3> getChunkCubes();

    /**
     * Sets the world that the selection is in.
     *
     * @return the world, or null
     */
    @Nullable
    World getWorld();

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

    //FAWE start
    default boolean contains(int x, int y, int z) {
        return contains(BlockVector3.at(x, y, z));
    }

    default boolean contains(int x, int z) {
        return contains(BlockVector3.at(x, 0, z));
    }

    default boolean isGlobal() {
        BlockVector3 pos1 = getMinimumPoint();
        BlockVector3 pos2 = getMaximumPoint();
        return pos1.x() == Integer.MIN_VALUE && pos1.z() == Integer.MIN_VALUE && pos2.x() == Integer.MAX_VALUE && pos2
                .z() == Integer.MAX_VALUE
                && pos1.y() <= WorldEdit
                .getInstance()
                .getPlatformManager()
                .queryCapability(
                        Capability.WORLD_EDITING)
                .versionMinY()
                && pos2.y() >= WorldEdit
                .getInstance()
                .getPlatformManager()
                .queryCapability(Capability.WORLD_EDITING)
                .versionMaxY();
    }

    default int getMinimumY() {
        return getMinimumPoint().y();
    }

    default int getMaximumY() {
        return getMaximumPoint().y();
    }

    default void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            boolean full
    ) {
        int minSection = Math.max(get.getMinSectionPosition(), getMinimumY() >> 4);
        int maxSection = Math.min(get.getMaxSectionPosition(), getMaximumY() >> 4);
        block = block.initChunk(chunk.getX(), chunk.getZ());
        for (int layer = minSection; layer <= maxSection; layer++) {
            if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
                return;
            }
            block = block.initLayer(get, set, layer);
            block.filter(filter, this);
        }
    }

    default void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            final int minY,
            final int maxY,
            boolean full
    ) {
        int minSection = minY >> 4;
        int maxSection = maxY >> 4;
        int yStart = (minY & 15);
        int yEnd = (maxY & 15);
        if (minSection == maxSection) {
            filter(chunk, filter, block, get, set, minSection, yStart, yEnd, full);
            return;
        }
        //If the yStart is not 0, the edit is smaller than the height of a ChunkSection, so filter individually and remove section as the minSection layer entry
        if (yStart != 0) {
            filter(chunk, filter, block, get, set, minSection, yStart, 15, full);
            minSection++;
        }
        //If the yEnd is not 15, the edit is smaller than the height of a ChunkSection, so filter individually and remove section as the maxSection layer entry
        if (yEnd != 15) {
            filter(chunk, filter, block, get, set, maxSection, 0, yEnd, full);
            maxSection--;
        }
        for (int layer = minSection; layer <= maxSection; layer++) {
            filter(chunk, filter, block, get, set, layer, full);
        }
    }

    default void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            int layer,
            boolean full
    ) {
        if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
            return;
        }
        block = block.initLayer(get, set, layer);
        block.filter(filter);
    }

    default void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            int layer,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            boolean full
    ) {
        if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
            return;
        }
        block = block.initLayer(get, set, layer);
        block.filter(filter, minX, minY, minZ, maxX, maxY, maxZ);
    }

    default void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            int layer,
            int yStart,
            int yEnd,
            boolean full
    ) {
        if ((!full && !get.hasSection(layer)) || !filter.appliesLayer(chunk, layer)) {
            return;
        }
        block = block.initLayer(get, set, layer);
        block.filter(filter, yStart, yEnd);
    }

    default boolean containsEntireCuboid(int bx, int tx, int by, int ty, int bz, int tz) {
        return contains(bx, by, bz)
                && contains(bx, by, tz)
                && contains(tx, by, bz)
                && contains(tx, by, tz)
                && contains(bx, ty, bz)
                && contains(bx, ty, tz)
                && contains(tx, ty, bz)
                && contains(tx, ty, tz);
    }

    default boolean containsChunk(int chunkX, int chunkZ) {
        int bx = chunkX << 4;
        int bz = chunkZ << 4;
        int tx = bx + 15;
        int tz = bz + 15;
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        return tx >= min.x() && bx <= max.x() && tz >= min.z() && bz <= max.z();
    }

    @Override
    default IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        if (tx >= min.x() && bx <= max.x() && tz >= min.z() && bz <= max.z()) {
            // contains some
            boolean processExtra = false;
            for (int layer = getMinimumY() >> 4; layer <= getMaximumY() >> 4; layer++) {
                if (!set.hasSection(layer)) {
                    continue;
                }
                int by = layer << 4;
                int ty = by + 15;
                if (!containsEntireCuboid(bx, tx, by, ty, bz, tz)) {
                    processExtra = true;
                    char[] arr = set.loadIfPresent(layer);
                    if (arr == null) {
                        continue;
                    }
                    for (int y = 0, index = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++, index++) {
                                if (arr[index] != BlockTypesCache.ReservedIDs.__RESERVED__ && !contains(bx + x, by + y, bz + z)) {
                                    arr[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                                }
                            }
                        }
                    }
                    set.setBlocks(layer, arr);
                }
            }
            if (processExtra) {
                BlockVector3 chunkPos = chunk.getChunkBlockCoord().withY(0);
                trimNBT(set, this::contains, pos -> this.contains(pos.add(chunkPos)));
            }
            return set;
        } else {
            return null;
        }
    }

    /**
     * Process the chunk, with the option to process as if the region is a blacklisted region, and thus any contained blocks
     * should be removed, rather than uncontained blocks being removed.
     *
     * @param asBlacklist If any blocks contained by the region should be removed
     */
    default IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set, boolean asBlacklist) {
        if (!asBlacklist) {
            return processSet(chunk, get, set);
        }
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        if (tx >= min.x() && bx <= max.x() && tz >= min.z() && bz <= max.z()) {
            // contains some
            boolean processExtra = false;
            for (int layer = getMinimumY() >> 4; layer <= getMaximumY() >> 4; layer++) {
                int by = layer << 4;
                int ty = by + 15;
                if (containsEntireCuboid(bx, tx, by, ty, bz, tz)) {
                    set.setBlocks(layer, FaweCache.INSTANCE.EMPTY_CHAR_4096);
                    processExtra = true;
                    continue;
                }
                char[] arr = set.load(layer);
                for (int y = 0, index = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++, index++) {
                            if (arr[index] != BlockTypesCache.ReservedIDs.__RESERVED__ && contains(x, y, z)) {
                                arr[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                                processExtra = true;
                            }
                        }
                    }
                }
                if (processExtra) {
                    set.setBlocks(layer, arr);
                }
            }
            if (processExtra) {
                BlockVector3 chunkPos = chunk.getChunkBlockCoord().withY(0);
                trimNBT(set, bv3 -> !this.contains(bv3), bv3 -> !this.contains(bv3.add(chunkPos)));
            }
            return set;
        } else {
            return null;
        }
    }

    @Override
    default Extent construct(Extent child) {
        if (isGlobal()) {
            return child;
        }
        return new SingleRegionExtent(child, FaweLimit.MAX, this);
    }

    @Override
    default ProcessorScope getScope() {
        return ProcessorScope.REMOVING_BLOCKS;
    }
    //FAWE end

}
