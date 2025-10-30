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

package com.sk89q.worldedit.extent;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.extent.clipboard.WorldCopyClipboard;
import com.fastasyncworldedit.core.extent.filter.block.ExtentFilterBlock;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.function.generator.CavesGen;
import com.fastasyncworldedit.core.function.generator.GenBase;
import com.fastasyncworldedit.core.function.generator.OreGen;
import com.fastasyncworldedit.core.function.generator.Resource;
import com.fastasyncworldedit.core.function.generator.SchemGen;
import com.fastasyncworldedit.core.function.pattern.MaskedPattern;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.registry.state.PropertyGroup;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.BlockCategoryMask;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MathUtils;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A world, portion of a world, clipboard, or other object that can have blocks
 * set or entities placed.
 *
 * @see InputExtent the get____() portion
 * @see OutputExtent the set____() portion
 */
public interface Extent extends InputExtent, OutputExtent {

    /**
     * Get the minimum point in the extent.
     *
     * <p>If the extent is unbounded, then a large (negative) value may
     * be returned.</p>
     *
     * @return the minimum point
     */
    BlockVector3 getMinimumPoint();

    /**
     * Get the maximum point in the extent.
     *
     * <p>If the extent is unbounded, then a large (positive) value may
     * be returned.</p>
     *
     * @return the maximum point
     */
    BlockVector3 getMaximumPoint();

    /**
     * Get a list of all entities within the given region.
     *
     * <p>If the extent is not wholly loaded (i.e. a world being simulated in the
     * game will not have every chunk loaded), then this list may not be
     * incomplete.</p>
     *
     * @param region the region in which entities must be contained
     * @return a list of entities
     */
    default List<? extends Entity> getEntities(Region region) {
        return Collections.emptyList();
    }

    /**
     * Get a list of all entities.
     *
     * <p>If the extent is not wholly loaded (i.e. a world being simulated in the
     * game will not have every chunk loaded), then this list may not be
     * incomplete.</p>
     *
     * @return a list of entities
     */
    default List<? extends Entity> getEntities() {
        return Collections.emptyList();
    }

    /**
     * Create an entity at the given location.
     *
     * @param entity   the entity
     * @param location the location
     * @return a reference to the created entity, or null if the entity could not be created
     */
    @Nullable
    default Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    //FAWE start

    /**
     * Create an entity at the given location, forcing a UUID onto the entity
     * <p>
     * Only use if you are aware of the consequences of forcing a UUID to an entity.
     *
     * @param entity   the entity
     * @param location the location
     * @param uuid     UUID to force the entity to have
     * @return a reference to the created entity, or null if the entity could not be created
     * @since 2.4.0
     */
    @Nullable
    default Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        return null;
    }

    /**
     * Create an entity at the given location.
     *
     * @param x    the x coordinate
     * @param y    the y coordinate
     * @param z    the z coordinate
     * @param uuid the unique identifier of the entity
     */
    default void removeEntity(int x, int y, int z, UUID uuid) {
    }

    /**
     * Removes all entities in the given region.
     *
     * @param region the region
     * @return the number of entities removed
     */
    default int removeEntities(Region region) {
        return this.getEntities(region).stream()
                .mapToInt(entity -> entity.remove() ? 1 : 0)
                .sum();
    }

    /*
    Queue based methods
    TODO NOT IMPLEMENTED:
     */
    default boolean isQueueEnabled() {
        return false;
    }

    default void enableQueue() {
        if (!isQueueEnabled()) {
            throw FaweException._enableQueue;
        }
    }

    default void disableQueue() {
        if (isQueueEnabled()) {
            throw FaweException._disableQueue;
        }
    }

    /*
    World based methods
    TODO NOT IMPLEMENTED:
     */

    default boolean isWorld() {
        return false;
    }

    default boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        return false;
    }

    /*
    Shifting operations down the pipeline from EditSession -> Extent
         - This allows certain extents (e.g. multithreaded extent) to override and optimize as needed
         - The EditSession shouldn't need to worry about implementation details
         - TODO: actually optimize these
     */

    /**
     * Returns the highest solid 'terrain' (movement-blocking) block.
     *
     * @param x    the X coordinate
     * @param z    the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    default int getHighestTerrainBlock(final int x, final int z, int minY, int maxY) {
        maxY = Math.min(maxY, getMaxY());
        minY = Math.max(getMinY(), minY);

        for (int y = maxY; y >= minY; --y) {
            BlockState block = getBlock(x, y, z);
            if (block.getBlockType().getMaterial().isMovementBlocker()) {
                return y;
            }
        }
        return minY;
    }

    /**
     * Returns the highest block matching the given mask.
     *
     * @param x      the X coordinate
     * @param z      the Z coordinate
     * @param minY   minimal height
     * @param maxY   maximal height
     * @param filter a mask of blocks to consider, or null to consider any solid (movement-blocking) block
     * @return height of highest block found or 'minY'
     */
    default int getHighestTerrainBlock(final int x, final int z, int minY, int maxY, Mask filter) {
        if (filter == null) {
            return getHighestTerrainBlock(x, z, minY, maxY);
        }
        maxY = Math.min(maxY, getMaxY());
        minY = Math.max(getMinY(), minY);

        MutableBlockVector3 mutable = new MutableBlockVector3();

        for (int y = maxY; y >= minY; --y) {
            if (filter.test(mutable.setComponents(x, y, z))) {
                return y;
            }
        }
        return minY;
    }

    /**
     * Returns the nearest surface layer (up/down from start), where a layer is 1/16th of a block to allow for snow, liquid, etc.
     *
     * @param x    x to search from
     * @param z    y to search from
     * @param y    z to search from
     * @param minY min y to search (inclusive)
     * @param maxY max y to search (inclusive)
     * @return nearest surface layer
     */
    default int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        maxY = Math.min(maxY, getMaxY());
        minY = Math.max(getMinY(), minY);

        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);

        BlockState block = getBlock(x, y, z);
        boolean state = !block.getBlockType().getMaterial().isMovementBlocker();
        int data1 = PropertyGroup.LEVEL.get(block);
        int data2 = data1;
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getBlock(x, y1, z);
            if (block.getBlockType().getMaterial().isMovementBlocker() == state) {
                return ((y1 - offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data1));
            }
            data1 = PropertyGroup.LEVEL.get(block);
            int y2 = y - d;
            block = getBlock(x, y2, z);
            if (block.getBlockType().getMaterial().isMovementBlocker() == state) {
                return ((y2 + offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data2));
            }
            data2 = PropertyGroup.LEVEL.get(block);
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getBlock(x, layer, z);
                    if (block.getBlockType().getMaterial().isMovementBlocker() == state) {
                        return (layer + offset) << 4;
                    }
                    data1 = PropertyGroup.LEVEL.get(block);
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getBlock(x, layer, z);
                    if (block.getBlockType().getMaterial().isMovementBlocker() == state) {
                        return ((layer - offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data2));
                    }
                    data2 = PropertyGroup.LEVEL.get(block);
                }
            }
        }
        return (state ? minY : maxY) << 4;
    }

    /**
     * Gets y value for the nearest block that is considered the surface of the terrain (cave roof/floor, mountain surface,
     * etc) where the block conforms to a given mask. Searches in the x,z column given.
     *
     * @param x         column x
     * @param z         column z
     * @param y         start y
     * @param minY      minimum y height to consider. Inclusive.
     * @param maxY      maximum y height to consider. Inclusive.
     * @param failedMin if nothing found, the minimum y value to return if returning min
     * @param failedMax if nothing found, the maximum y value to return if returning max
     * @param mask      mask to test blocks against
     * @return The y value of the nearest terrain block
     */
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        maxY = Math.min(maxY, getMaxY());
        minY = Math.max(getMinY(), minY);

        y = Math.max(minY, Math.min(maxY, y));
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        BlockVector3 pos = MutableBlockVector3.get(x, y, z);
        boolean state = !mask.test(pos);
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            if (mask.test(pos.mutY(y1)) != state) {
                return y1 - offset;
            }
            int y2 = y - d;
            if (mask.test(pos.mutY(y2)) != state) {
                return y2 + offset;
            }
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    if (mask.test(pos.mutY(layer)) != state) {
                        return layer + offset;
                    }
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    if (mask.test(pos.mutY(layer)) != state) {
                        return layer - offset;
                    }
                }
            }
        }
        return state ? failedMin : failedMax;
    }

    /**
     * Gets y value for the nearest block that is considered the surface of the terrain (cave roof/floor, mountain surface,
     * etc). Searches in the x,z column given.
     *
     * @param x         column x
     * @param z         column z
     * @param y         start y
     * @param minY      minimum y height to consider. Inclusive.
     * @param maxY      maximum y height to consider. Inclusive.
     * @param ignoreAir if air at the final value if no block found should be considered for return, else return -1
     * @return The y value of the nearest terrain block
     */
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY, ignoreAir);
    }

    /**
     * Gets y value for the nearest block that is considered the surface of the terrain (cave roof/floor, mountain surface,
     * etc). Searches in the x,z column given.
     *
     * @param x    column x
     * @param z    column z
     * @param y    start y
     * @param minY minimum y height to consider. Inclusive.
     * @param maxY maximum y height to consider. Inclusive.
     * @return The y value of the nearest terrain block
     */
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY);
    }

    /**
     * Gets y value for the nearest block that is considered the surface of the terrain (cave roof/floor, mountain surface,
     * etc). Searches in the x,z column given.
     *
     * @param x         column x
     * @param z         column z
     * @param y         start y
     * @param minY      minimum y height to consider. Inclusive.
     * @param maxY      maximum y height to consider. Inclusive.
     * @param failedMin if nothing found, the minimum y value to return if returning min
     * @param failedMax if nothing found, the maximum y value to return if returning max
     * @return The y value of the nearest terrain block
     */
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, true);
    }

    /**
     * Gets y value for the nearest block that is considered the surface of the terrain (cave roof/floor, mountain surface,
     * etc). Searches in the x,z column given.
     *
     * @param x         column x
     * @param z         column z
     * @param y         start y
     * @param minY      minimum y height to consider. Inclusive.
     * @param maxY      maximum y height to consider. Inclusive.
     * @param failedMin if nothing found, the minimum y value to return if returning min
     * @param failedMax if nothing found, the maximum y value to return if returning max
     * @param ignoreAir if air at the final value if no block found should be considered for return, else return -1
     * @return The y value of the nearest terrain block
     */
    default int getNearestSurfaceTerrainBlock(
            int x,
            int z,
            int y,
            int minY,
            int maxY,
            int failedMin,
            int failedMax,
            boolean ignoreAir
    ) {
        maxY = Math.min(maxY, getMaxY());
        minY = Math.max(getMinY(), minY);

        y = Math.max(minY, Math.min(maxY, y));
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        BlockState block = getBlock(x, y, z);
        boolean state = !SolidBlockMask.isSolid(block);
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getBlock(x, y1, z);
            if (matchesSolidState(block, state)) {
                return y1 - offset;
            }
            int y2 = y - d;
            block = getBlock(x, y2, z);
            if (matchesSolidState(block, state)) {
                return y2 + offset;
            }
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getBlock(x, layer, z);
                    if (matchesSolidState(block, state)) {
                        return layer + offset;
                    }
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getBlock(x, layer, z);
                    if (matchesSolidState(block, state)) {
                        return layer - offset;
                    }
                }
            }
        }
        int result = state ? failedMin : failedMax;
        if (result > minY && !ignoreAir) {
            block = getBlock(x, result, z);
            return block.getBlockType().getMaterial().isAir() ? -1 : result;
        }
        return result;
    }

    private static boolean matchesSolidState(BlockState block, boolean state) {
        return SolidBlockMask.isSolid(block) == state && block.getBlockType() != BlockTypes.__RESERVED__;
    }

    default void addCaves(Region region) throws WorldEditException {
        generate(region, new CavesGen(8));
    }

    default void generate(Region region, GenBase gen) throws WorldEditException {
        for (BlockVector2 chunkPos : region.getChunks()) {
            gen.generate(chunkPos, this);
        }
    }

    default void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws
            WorldEditException {
        spawnResource(region, new SchemGen(mask, this, clipboards, rotate, region), rarity, 1);
    }

    default void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (BlockVector2 chunkPos : region.getChunks()) {
            for (int i = 0; i < frequency; i++) {
                if (random.nextInt(100) > rarity) {
                    continue;
                }
                int x = (chunkPos.x() << 4) + random.nextInt(16);
                int z = (chunkPos.z() << 4) + random.nextInt(16);
                gen.spawn(random, x, z);
            }
        }
    }

    /**
     * Returns true if the extent contains the given position
     *
     * @param pt position
     * @return if position is contained
     */
    default boolean contains(BlockVector3 pt) {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        return pt.containedWithin(min, max);
    }

    /**
     * Returns true if the extent contains the given position
     *
     * @param x position x
     * @param y position y
     * @param z position z
     * @return if position is contained
     */
    default boolean contains(int x, int y, int z) {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        return min.x() <= x && max.x() >= x
                && min.y() <= y && max.y() >= y
                && min.z() <= z && max.z() >= z;
    }

    default void addOre(
            Region region,
            Mask mask,
            Pattern material,
            int size,
            int frequency,
            int rarity,
            int minY,
            int maxY
    ) throws WorldEditException {
        addOre(region, mask, material, size, frequency, rarity, minY, maxY, false);
    }

    /**
     * Generate ore-like deposits with the given pattern and settings
     *
     * @param region     region to generate in
     * @param mask       mask of where to place
     * @param material   pattern to place
     * @param size       maximum size of deposits
     * @param frequency  number of times to attempt to place a deposit
     * @param rarity     percentage chance of generating a deposit per attempt
     * @param minY       min Y to consider generation from (important for triangular generation)
     * @param maxY       max Y to consider generation from (important for triangular generation)
     * @param triangular if a triangular distribution of ores should be used (rather than flat)
     * @throws WorldEditException on error
     * @since TODO
     */
    default void addOre(
            Region region,
            Mask mask,
            Pattern material,
            int size,
            int frequency,
            int rarity,
            int minY,
            int maxY,
            boolean triangular
    ) throws WorldEditException {
        spawnResource(
                region,
                new OreGen(this, mask, material, size, minY, maxY, triangular),
                rarity,
                frequency
        );
    }

    default void addOres(Region region, Mask mask) throws WorldEditException {
        addOres(region, mask, false, false);
    }

    /**
     * Generator a distribution of ore deposits similar to vanilla generation
     *
     * @param region                  region to generate in
     * @param mask                    mask of where to place
     * @param deepslateBelowZero      if ores should be their deepslate equivalent below zero (overrides deepslateWhereDeepslate)
     * @param deepslateWhereDeepslate if ores should be their deepslate equivalent if the existing block is deepslate
     * @throws WorldEditException on error
     * @since TODO
     */
    default void addOres(Region region, Mask mask, boolean deepslateBelowZero, boolean deepslateWhereDeepslate) throws
            WorldEditException {
        mask = new MaskIntersection(new RegionMask(region), mask);

        BiFunction<BlockType, BlockType, Pattern> patternFunc;

        if (deepslateBelowZero) {
            patternFunc = (ore, deepslate_ore) -> new MaskedPattern(
                    new Mask() {

                        @Override
                        public boolean test(final BlockVector3 vector) {
                            return vector.y() < 0;
                        }

                        @Override
                        public Mask copy() {
                            return this;
                        }
                    }, deepslate_ore, ore
            );
        } else if (deepslateWhereDeepslate) {
            patternFunc = (ore, deepslate_ore) -> new MaskedPattern(
                    new BlockCategoryMask(
                            this,
                            BlockCategories.DEEPSLATE_ORE_REPLACEABLES
                    ), deepslate_ore, ore
            );
        } else {
            patternFunc = (ore, deepslate_ore) -> ore;
        }

        addOre(region, mask, BlockTypes.GRAVEL, 33, 14, 100, getMinY(), getMaxY(), false);

        addOre(region, mask, BlockTypes.ANDESITE, 33, 2, 100, 0, 60, false); // Lower
        addOre(region, mask, BlockTypes.ANDESITE, 33, 1, 17, 64, 128, false); // Upper

        addOre(region, mask, BlockTypes.DIORITE, 33, 2, 100, 0, 60, false); // Lower
        addOre(region, mask, BlockTypes.DIORITE, 33, 1, 17, 64, 128, false); // Upper

        addOre(region, mask, BlockTypes.GRANITE, 33, 2, 100, 0, 60, false); // Lower
        addOre(region, mask, BlockTypes.GRANITE, 33, 1, 17, 64, 128, false); // Upper

        addOre(region, mask, BlockTypes.TUFF, 33, 2, 100, getMinY(), 0, false);

        addOre(region, mask, BlockTypes.DIRT, 33, 7, 100, 0, 160, false);

        addOre( // Lower
                region,
                mask,
                patternFunc.apply(BlockTypes.COAL_ORE, BlockTypes.DEEPSLATE_COAL_ORE),
                17,
                20,
                100,
                0,
                192,
                true
        );
        addOre( // Upper
                region,
                mask,
                patternFunc.apply(BlockTypes.COAL_ORE, BlockTypes.DEEPSLATE_COAL_ORE),
                17,
                30,
                100,
                136,
                getMaxY(),
                false
        );

        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.COPPER_ORE, BlockTypes.DEEPSLATE_COPPER_ORE),
                13,
                16,
                100,
                -16,
                112,
                true
        );

        addOre( // Small
                region,
                mask,
                patternFunc.apply(BlockTypes.IRON_ORE, BlockTypes.DEEPSLATE_IRON_ORE),
                9,
                10,
                100,
                getMinY(),
                72,
                false
        ); // Middle
        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.IRON_ORE, BlockTypes.DEEPSLATE_IRON_ORE),
                9,
                10,
                100,
                -24,
                56,
                true
        );
        addOre( // Upper
                region,
                mask,
                patternFunc.apply(BlockTypes.IRON_ORE, BlockTypes.DEEPSLATE_IRON_ORE),
                9,
                90,
                100,
                80,
                384,
                true
        );

        addOre( // Lower
                region,
                mask,
                patternFunc.apply(BlockTypes.GOLD_ORE, BlockTypes.DEEPSLATE_GOLD_ORE),
                9,
                1,
                50,
                -64
                ,
                -48,
                false
        );
        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.GOLD_ORE, BlockTypes.DEEPSLATE_GOLD_ORE),
                9,
                4,
                100,
                -64,
                32,
                true
        );

        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.REDSTONE_ORE, BlockTypes.DEEPSLATE_REDSTONE_ORE),
                8,
                8,
                100,
                -32,
                32,
                true
        );
        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.REDSTONE_ORE, BlockTypes.DEEPSLATE_REDSTONE_ORE),
                8,
                4,
                100,
                getMinY(),
                15,
                false
        );

        int diaMin = getMinY() - 80;
        int diaMax = getMinY() + 80;
        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.DIAMOND_ORE, BlockTypes.DEEPSLATE_DIAMOND_ORE),
                5,
                7,
                100,
                diaMin,
                diaMax,
                true
        );
        addOre( // Medium
                region,
                mask,
                patternFunc.apply(BlockTypes.DIAMOND_ORE, BlockTypes.DEEPSLATE_DIAMOND_ORE),
                8,
                2,
                100,
                -64,
                -4,
                false
        );
        addOre( // Large
                region,
                mask,
                patternFunc.apply(BlockTypes.DIAMOND_ORE, BlockTypes.DEEPSLATE_DIAMOND_ORE),
                23,
                1,
                11,
                diaMin,
                diaMax,
                true
        );
        addOre( // Buried
                region,
                mask,
                patternFunc.apply(BlockTypes.DIAMOND_ORE, BlockTypes.DEEPSLATE_DIAMOND_ORE),
                10,
                4,
                100,
                diaMin,
                diaMax,
                true
        );

        addOre(region, mask, patternFunc.apply(BlockTypes.LAPIS_ORE, BlockTypes.DEEPSLATE_LAPIS_ORE), 7, 2, 100, -32, 32, true);
        addOre( // Buried
                region,
                mask,
                patternFunc.apply(BlockTypes.LAPIS_ORE, BlockTypes.DEEPSLATE_LAPIS_ORE),
                7,
                4,
                100,
                getMinY(),
                64,
                false
        );

        addOre(
                region,
                mask,
                patternFunc.apply(BlockTypes.EMERALD_ORE, BlockTypes.DEEPSLATE_EMERALD_ORE),
                5,
                100,
                100,
                -16,
                480,
                true
        );
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @return the results
     */
    default List<Countable<BlockType>> getBlockDistribution(final Region region) {
        int[] counter = new int[BlockTypes.size()];

        for (final BlockVector3 pt : region) {
            BlockType type = getBlock(pt).getBlockType();
            if (type == BlockTypes.__RESERVED__) {
                counter[1]++;
                continue;
            }
            counter[type.getInternalId()]++;
        }
        List<Countable<BlockType>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<>(BlockTypes.get(i), count));
            }
        }
        Collections.sort(distribution);
        return distribution;
    }

    /**
     * Get the block distribution (with data values) inside a region.
     *
     * @param region a region
     * @return the results
     */
    default List<Countable<BlockState>> getBlockDistributionWithData(final Region region) {
        int[][] counter = new int[BlockTypes.size()][];

        for (final BlockVector3 pt : region) {
            BlockState blk = this.getBlock(pt);
            BlockType type = blk.getBlockType();
            if (type == BlockTypes.__RESERVED__) {
                int[] stateCounter = counter[1];
                if (stateCounter == null) {
                    counter[1] = stateCounter = new int[BlockTypes.AIR.getMaxStateId() + 1];
                }
                stateCounter[BlockTypes.AIR.getDefaultState().getInternalPropertiesId()]++;
            }
            int[] stateCounter = counter[type.getInternalId()];
            if (stateCounter == null) {
                counter[type.getInternalId()] = stateCounter = new int[type.getMaxStateId() + 1];
            }
            stateCounter[blk.getInternalPropertiesId()]++;
        }
        List<Countable<BlockState>> distribution = new ArrayList<>();
        for (int typeId = 0; typeId < counter.length; typeId++) {
            BlockType type = BlockTypes.get(typeId);
            int[] stateCount = counter[typeId];
            if (stateCount != null) {
                for (int propId = 0; propId < stateCount.length; propId++) {
                    int count = stateCount[propId];
                    if (count != 0) {
                        BlockState state = type.withPropertyId(propId);
                        distribution.add(new Countable<>(state, count));
                    }

                }
            }
        }
        return distribution;
    }

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }

    default boolean cancel() {
        ExtentTraverser<Extent> traverser = new ExtentTraverser<>(this);

        NullExtent nullExtent = new NullExtent(this, FaweCache.MANUAL);

        ExtentTraverser<Extent> next = traverser.next();
        if (next != null) {
            Extent child = next.get();
            if (child instanceof NullExtent) {
                return true;
            }
            traverser.setNext(nullExtent);
            child.cancel();
        }
        addProcessor(nullExtent);
        addPostProcessor(nullExtent);
        return true;
    }

    default int getMinY() {
        return getMinimumPoint().y();
    }

    default int getMaxY() {
        return getMaximumPoint().y();
    }

    /**
     * Lazily copy a region
     *
     * @param region
     * @return
     */
    default Clipboard lazyCopy(Region region) {
        WorldCopyClipboard faweClipboard = WorldCopyClipboard.of(this, region);
        faweClipboard.setOrigin(region.getMinimumPoint());
        return faweClipboard;
    }


    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region       the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the block
     */
    default int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        BlockMask mask = new BlockMask(this, searchBlocks);
        return countBlocks(region, mask);
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region     the region
     * @param searchMask mask to match
     * @return the number of blocks that matched the mask
     */
    default int countBlocks(Region region, Mask searchMask) {
        //FAWE start > use slightly more performant RegionVisitor
        RegionVisitor visitor = new RegionVisitor(region, searchMask::test, this);
        //FAWE end
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    /**
     * Sets all the blocks inside a region to a given block type.
     *
     * @param region the region
     * @param block  the block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(block);

        int changes = 0;
        for (BlockVector3 pos : region) {
            if (setBlock(pos, block)) {
                changes++;
            }
        }
        return changes;
    }

    /**
     * Sets all the blocks inside a region to a given pattern.
     *
     * @param region  the region
     * @param pattern the pattern that provides the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);
        if (pattern instanceof BlockPattern) {
            return setBlocks(region, ((BlockPattern) pattern).getBlock());
        }
        if (pattern instanceof BlockStateHolder) {
            return setBlocks(region, (BlockStateHolder) pattern);
        }
        int count = 0;
        for (BlockVector3 pos : region) {
            if (pattern.apply(this, pos, pos)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region      the region to replace the blocks within
     * @param filter      a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws
            MaxChangedBlocksException {
        return replaceBlocks(region, filter, (Pattern) replacement);
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region  the region to replace the blocks within
     * @param filter  a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        Mask mask = filter == null ? new ExistingBlockMask(this) : new BlockMask(this, filter);
        return replaceBlocks(region, mask, pattern);
    }

    /**
     * Replaces all the blocks matching a given mask, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region  the region to replace the blocks within
     * @param mask    the mask that blocks must match
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(mask);
        checkNotNull(pattern);

        BlockReplace replace = new BlockReplace(this, pattern);
        RegionMaskingFilter filter = new RegionMaskingFilter(mask, replace);
        //FAWE start > add extent to RegionVisitor to allow chunk preloading
        RegionVisitor visitor = new RegionVisitor(region, filter, this);
        //FAWE end
        Operations.completeLegacy(visitor);
        return visitor.getAffected();
    }


    /**
     * Sets the blocks at the center of the given region to the given pattern.
     * If the center sits between two blocks on a certain axis, then two blocks
     * will be placed to mark the center.
     *
     * @param region  the region to find the center of
     * @param pattern the replacement pattern
     * @return the number of blocks placed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    default int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        Vector3 center = region.getCenter();
        Region centerRegion = new CuboidRegion(
                this instanceof World ? (World) this : null, // Causes clamping of Y range
                BlockVector3.at(((int) center.x()), ((int) center.y()), ((int) center.z())),
                BlockVector3.at(MathUtils.roundHalfUp(center.x()),
                        center.y(), MathUtils.roundHalfUp(center.z())
                )
        );
        return setBlocks(centerRegion, pattern);
    }

    default int setBlocks(final Set<BlockVector3> vset, final Pattern pattern) {
        if (vset instanceof Region) {
            return setBlocks((Region) vset, pattern);
        }
        int count = 0;
        for (BlockVector3 pos : vset) {
            if (pattern.apply(this, pos, pos)) {
                count++;
            }
        }
        return count;
    }

    default boolean relight(int x, int y, int z) {
        return false;
    }

    default boolean relightBlock(int x, int y, int z) {
        return false;
    }

    default boolean relightSky(int x, int y, int z) {
        return false;
    }

    /**
     * Have an extent processed
     * - Either block (Extent) processing or chunk processing
     *
     * @param processor
     * @return processed Extent
     */
    default Extent addProcessor(IBatchProcessor processor) {
        return processor.construct(this);
    }

    default Extent addPostProcessor(IBatchProcessor processor) {
        if (processor.getScope() != ProcessorScope.READING_BLOCKS) {
            throw new IllegalArgumentException("You cannot alter blocks in a PostProcessor");
        }
        return processor.construct(this);
    }

    default Extent enableHistory(AbstractChangeSet changeSet) {
        if (Settings.settings().HISTORY.SEND_BEFORE_HISTORY) {
            return addPostProcessor(changeSet);
        } else {
            return addProcessor(changeSet);
        }
    }

    default Extent disableHistory() {
        return this;
    }

    default <T extends Filter> T apply(Region region, T filter, boolean full) {
        return apply(region, filter);
    }

    default <T extends Filter> T apply(Iterable<BlockVector3> positions, T filter) {
        ExtentFilterBlock block = new ExtentFilterBlock(this);
        for (BlockVector3 pos : positions) {
            filter.applyBlock(block.init(pos));
        }
        return filter;
    }
    //FAWE end
}
