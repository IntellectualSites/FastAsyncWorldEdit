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

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public class AbstractDelegateExtent implements Extent, LightingExtent {
    private final Extent extent;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public AbstractDelegateExtent(Extent extent) {
        checkNotNull(extent);
        this.extent = extent;
    }

    /**
     * Get the extent.
     *
     * @return the extent
     */
    public Extent getExtent() {
        return extent;
    }

    /*
    Bounds
     */

    @Override
    public BlockVector3 getMinimumPoint() {
        return extent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return extent.getMaximumPoint();
    }

    @Override
    public int getMaxY() {
        return extent.getMaxY();
    }

    /*
    Input + Output
     */

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return extent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return extent.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return extent.getBiomeType(x, z);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return extent.setBiome(x, y, z, biome);
    }

    /*
    Light
     */

    public int getSkyLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getSkyLight(x, y, z);
        }
        return 0;
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
        return getBlock(x, y, z).getBlockType().getMaterial().getLightOpacity();
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
        return getBlock(x, y, z).getBlockType().getMaterial().getLightValue();
    }

    /*
    Generic
     */

    @Override
    public String toString() {
        return super.toString() + ":" + extent.toString();
    }

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public @Nullable Operation commit() {
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
