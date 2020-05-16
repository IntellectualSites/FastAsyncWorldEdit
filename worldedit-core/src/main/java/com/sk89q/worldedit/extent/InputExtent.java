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

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Provides the current state of blocks, entities, and so on.
 */
public interface InputExtent {

    /**
     * Get a snapshot of the block at the given location.
     *
     * <p>If the given position is out of the bounds of the extent, then the behavior
     * is undefined (an air block could be returned). However, {@code null}
     * should <strong>not</strong> be returned.</p>
     *
     * <p>The returned block is immutable and is a snapshot of the block at the time
     * of call. It has no position attached to it, so it could be reused in
     * {@link Pattern}s and so on.</p>
     *
     * @param position position of the block
     * @return the block
     */
    default BlockState getBlock(BlockVector3 position) {
        return getBlock(position.getX(), position.getY(), position.getZ());
    }

    default BlockState getBlock(int x, int y, int z) {
        return getBlock(MutableBlockVector3.get(x, y, z));
    }

    /**
     * Get a immutable snapshot of the block at the given location.
     *
     * @param position position of the block
     * @return the block
     */
    default BaseBlock getFullBlock(BlockVector3 position) {
        return getFullBlock(position.getX(), position.getY(), position.getZ());
    }

    default BaseBlock getFullBlock(int x, int y, int z) {
        return getFullBlock(MutableBlockVector3.get(x, y, z));
    }

    /**
     * Get the biome at the given location.
     *
     * <p>If there is no biome available, then the ocean biome should be
     * returned.</p>
     *
     * @param position the (x, z) location to check the biome at
     * @return the biome at the location
     */
    default BiomeType getBiome(BlockVector2 position) {
        return getBiomeType(position.getX(), 0, position.getZ());
    }

    default BiomeType getBiomeType(int x, int y, int z) {
        return getBiome(MutableBlockVector2.get(x, z));
    }

    /**
     * Get the light level at the given location
     *
     * @param position location
     * @return the light level at the location
     */
    default int getEmmittedLight(MutableBlockVector3 position) {
        return getEmmittedLight(position.getX(), position.getY(), position.getZ());
    }

    default int getEmmittedLight(int x, int y, int z) {
        return 0;
    }

    /**
     * Get the sky light level at the given location
     *
     * @param position location
     * @return the sky light level at the location
     */
    default int getSkyLight(MutableBlockVector3 position) {
        return getSkyLight(position.getX(), position.getY(), position.getZ());
    }

    default int getSkyLight(int x, int y, int z) {
        return 0;
    }

    default int getBrightness(MutableBlockVector3 position) {
        return getBrightness(position.getX(), position.getY(), position.getZ());
    }

    default int getBrightness(int x, int y, int z) {
        return getFullBlock(x, y, z).getMaterial().getLightValue();
    }

    default int getOpacity(MutableBlockVector3 position) {
        return getOpacity(position.getX(), position.getY(), position.getZ());
    }

    default int getOpacity(int x, int y, int z) {
        return getFullBlock(x, y, z).getMaterial().getLightOpacity();
    }
}
