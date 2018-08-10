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

import com.boydti.fawe.jnbt.anvil.generator.GenBase;
import com.boydti.fawe.jnbt.anvil.generator.Resource;
import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public class AbstractDelegateExtent implements LightingExtent {

    private transient final Extent extent;
//    private MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public AbstractDelegateExtent(Extent extent) {
        checkNotNull(extent);
        this.extent = extent;
    }

    public int getSkyLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getSkyLight(x, y, z);
        }
        return 0;
    }

    @Override
    public int getMaxY() {
        return extent.getMaxY();
    }

    @Override
    public BlockType getBlockType(BlockVector3 position) {
        return extent.getBlockType(position);
    }

    @Override
    public BlockState getFullBlock(BlockVector3 position) {
        return extent.getFullBlock(position);
    }


    public int getBlockLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getBlockLight(x, y, z);
        }
        return getBrightness(x, y, z);
    }

    public int getOpacity(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getOpacity(x, y, z);
        }
        return getLazyBlock(x, y, z).getBlockType().getMaterial().getLightOpacity();
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getLight(x, y, z);
        }
        return 0;
    }

    public int getBrightness(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getBrightness(x, y, z);
        }
        return getLazyBlock(x, y, z).getBlockType().getMaterial().getLightValue();
    }

    /**
     * Get the extent.
     *
     * @return the extent
     */
    public Extent getExtent() {
        return extent;
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
//        mutable.mutX(x);
//        mutable.mutY(y);
//        mutable.mutZ(z);
        return extent.getLazyBlock(BlockVector3.at(x, y, z));
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return extent.getLazyBlock(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
//        mutable.mutX(x);
//        mutable.mutY(y);
//        mutable.mutZ(z);
        return setBlock(BlockVector3.at(x, y, z), block);
    }
    
    public BlockState getBlock(BlockVector3 position) {
        return extent.getBlock(position);
    }

    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return extent.setBlock(location, block);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return extent.createEntity(location, entity);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return extent.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return extent.getEntities(region);
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return extent.getBiome(position);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return extent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome biome) {
        return extent.setBiome(x, y, z, biome);
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return extent.getHighestTerrainBlock(x, z, minY, maxY);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return extent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return extent.getMaximumPoint();
    }

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + extent.toString();
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return extent.getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
    }

    @Override
    public void addCaves(Region region) throws WorldEditException {
        extent.addCaves(region);
    }

    @Override
    public void generate(Region region, GenBase gen) throws WorldEditException {
        extent.generate(region, gen);
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        extent.spawnResource(region, gen, rarity, frequency);
    }

    @Override
    public boolean contains(BlockVector3 pt) {
        return extent.contains(pt);
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        extent.addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    public void addOres(Region region, Mask mask) throws WorldEditException {
        extent.addOres(region, mask);
    }

    @Override
    public @Nullable
    Operation commit() {
        Operation ours = commitBefore();
        Operation other = null;
        if (extent != this) other = extent.commit();
        if (ours != null && other != null) {
            return new OperationQueue(ours, other);
        } else if (ours != null) {
            return ours;
        } else if (other != null) {
            return other;
        } else {
            return null;
        }
    }


}
