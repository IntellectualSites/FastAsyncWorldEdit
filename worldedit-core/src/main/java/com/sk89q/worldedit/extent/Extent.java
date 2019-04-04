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

package com.sk89q.worldedit.extent;

import com.boydti.fawe.jnbt.anvil.generator.*;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.clipboard.WorldCopyClipboard;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.PropertyGroup;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
     * @param entity the entity
     * @param location the location
     * @return a reference to the created entity, or null if the entity could not be created
     */
    default @Nullable Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    default BlockState getBlock(BlockVector3 position) {
        return getFullBlock(position).toImmutableState();
    }

    @Override
    default BlockState getLazyBlock(BlockVector3 position) {
        return getFullBlock(position).toImmutableState();
    }

    default BlockState getLazyBlock(int x, int y, int z) {
        return getLazyBlock(BlockVector3.at(x, y, z));
    }

    default <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T state) throws WorldEditException {
        return setBlock(BlockVector3.at(x, y, z), state);
    }

    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        return setBiome(BlockVector2.at(x, z), biome);
    }

    default int getHighestTerrainBlock(final int x, final int z, int minY, int maxY) {
        maxY = Math.min(maxY, Math.max(0, maxY));
        minY = Math.max(0, minY);
        for (int y = maxY; y >= minY; --y) {
            BlockState block = getLazyBlock(x, y, z);
            if (block.getBlockType().getMaterial().isMovementBlocker()) {
                return y;
            }
        }
        return minY;
    }

    default int getHighestTerrainBlock(final int x, final int z, int minY, int maxY, Mask filter) {
        maxY = Math.min(maxY, Math.max(0, maxY));
        minY = Math.max(0, minY);
        for (int y = maxY; y >= minY; --y) {
            if (filter.test(MutableBlockVector3.get(x, y, z))) {
                return y;
            }
        }
        return minY;
    }

    default int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);

        BlockState block = getLazyBlock(x, y, z);
        boolean state = !block.getBlockType().getMaterial().isMovementBlocker();
        int data1 = PropertyGroup.LEVEL.get(block);
        int data2 = data1;
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getLazyBlock(x, y1, z);
            if (!block.getBlockType().getMaterial().isMovementBlocker() != state) {
                return ((y1 - offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data1));
            }
            data1 = PropertyGroup.LEVEL.get(block);
            int y2 = y - d;
            block = getLazyBlock(x, y2, z);
            if (!block.getBlockType().getMaterial().isMovementBlocker() != state) {
                return ((y2 + offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data2));
            }
            data2 = PropertyGroup.LEVEL.get(block);
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getLazyBlock(x, layer, z);
                    if (!block.getBlockType().getMaterial().isMovementBlocker() != state) {
                        int data = (state ? PropertyGroup.LEVEL.get(block) : data1);
                        return ((layer + offset) << 4) + 0;
                    }
                    data1 = PropertyGroup.LEVEL.get(block);
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getLazyBlock(x, layer, z);
                    if (!block.getBlockType().getMaterial().isMovementBlocker() != state) {
                        return ((layer - offset) << 4) - (15 - (state ? PropertyGroup.LEVEL.get(block) : data2));
                    }
                    data2 = PropertyGroup.LEVEL.get(block);
                }
            }
        }
        return (state ? minY : maxY) << 4;
    }

    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY, ignoreAir);
    }

    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY);
    }

    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, true);
    }

    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        y = Math.max(minY, Math.min(maxY, y));
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        boolean state = !mask.test(MutableBlockVector3.get(x, y, z));
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            if (mask.test(MutableBlockVector3.get(x, y1, z)) != state) return y1 - offset;
            int y2 = y - d;
            if (mask.test(MutableBlockVector3.get(x, y2, z)) != state) return y2 + offset;
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    if (mask.test(MutableBlockVector3.get(x, layer, z)) != state) return layer + offset;
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    if (mask.test(MutableBlockVector3.get(x, layer, z)) != state) return layer - offset;
                }
            }
        }
        int result = state ? failedMin : failedMax;
        return result;
    }

    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        y = Math.max(minY, Math.min(maxY, y));
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        BlockStateHolder block = getLazyBlock(x, y, z);
        boolean state = !block.getBlockType().getMaterial().isMovementBlocker();
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getLazyBlock(x, y1, z);
            if (!block.getMaterial().isMovementBlocker() != state && block.getBlockType() != BlockTypes.__RESERVED__) return y1 - offset;
            int y2 = y - d;
            block = getLazyBlock(x, y2, z);
            if (!block.getMaterial().isMovementBlocker() != state && block.getBlockType() != BlockTypes.__RESERVED__) return y2 + offset;
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getLazyBlock(x, layer, z);
                    if (!block.getMaterial().isMovementBlocker() != state && block.getBlockType() != BlockTypes.__RESERVED__) return layer + offset;
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getLazyBlock(x, layer, z);
                    if (!block.getMaterial().isMovementBlocker() != state && block.getBlockType() != BlockTypes.__RESERVED__) return layer - offset;
                }
            }
        }
        int result = state ? failedMin : failedMax;
        if(result > 0 && !ignoreAir) {
            block = getLazyBlock(x, result, z);
            return block.getBlockType().getMaterial().isAir() ? -1 : result;
        }
        return result;
    }

    default void addCaves(Region region) throws WorldEditException {
        generate(region, new CavesGen(8));
    }

    default void generate(Region region, GenBase gen) throws WorldEditException {
        for (BlockVector2 chunkPos : region.getChunks()) {
            gen.generate(chunkPos, this);
        }
    }

    default public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        spawnResource(region, new SchemGen(mask, this, clipboards, rotate), rarity, 1);
    }

    default void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (BlockVector2 chunkPos : region.getChunks()) {
            for (int i = 0; i < frequency; i++) {
                if (random.nextInt(100) > rarity) {
                    continue;
                }
                int x = (chunkPos.getBlockX() << 4) + random.nextInt(16);
                int z = (chunkPos.getBlockZ() << 4) + random.nextInt(16);
                gen.spawn(random, x, z);
            }
        }
    }

    default boolean contains(BlockVector3 pt) {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        return (pt.containedWithin(min, max));
    }

    default public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        spawnResource(region, new OreGen(this, mask, material, size, minY, maxY), rarity, frequency);
    }

    default public void addOres(Region region, Mask mask) throws WorldEditException {
        addOre(region, mask, BlockTypes.DIRT.getDefaultState(), 33, 10, 100, 0, 255);
        addOre(region, mask, BlockTypes.GRAVEL.getDefaultState(), 33, 8, 100, 0, 255);
        addOre(region, mask, BlockTypes.ANDESITE.getDefaultState(), 33, 10, 100, 0, 79);
        addOre(region, mask, BlockTypes.DIORITE.getDefaultState(), 33, 10, 100, 0, 79);
        addOre(region, mask, BlockTypes.GRANITE.getDefaultState(), 33, 10, 100, 0, 79);
        addOre(region, mask, BlockTypes.COAL_ORE.getDefaultState(), 17, 20, 100, 0, 127);
        addOre(region, mask, BlockTypes.IRON_ORE.getDefaultState(), 9, 20, 100, 0, 63);
        addOre(region, mask, BlockTypes.GOLD_ORE.getDefaultState(), 9, 2, 100, 0, 31);
        addOre(region, mask, BlockTypes.REDSTONE_ORE.getDefaultState(), 8, 8, 100, 0, 15);
        addOre(region, mask, BlockTypes.DIAMOND_ORE.getDefaultState(), 8, 1, 100, 0, 15);
        addOre(region, mask, BlockTypes.LAPIS_ORE.getDefaultState(), 7, 1, 100, 0, 15);
        addOre(region, mask, BlockTypes.EMERALD_ORE.getDefaultState(), 5, 1, 100, 4, 31);
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
            BlockType type = getBlockType(pt);
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
        // Collections.reverse(distribution);
        return distribution;
    }

    /**
     * Lazily copy a region
     *
     * @param region
     * @return
     */
    default BlockArrayClipboard lazyCopy(Region region) {
        WorldCopyClipboard faweClipboard = new WorldCopyClipboard(this, region);
        BlockArrayClipboard weClipboard = new BlockArrayClipboard(region, faweClipboard);
        weClipboard.setOrigin(region.getMinimumPoint());
        return weClipboard;
    }

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }

    default int getMaxY() {
        return 255;
    }
}
