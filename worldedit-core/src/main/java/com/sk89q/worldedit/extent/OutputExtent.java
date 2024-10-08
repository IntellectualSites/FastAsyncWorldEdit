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

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.internal.util.DeprecationUtil;
import com.sk89q.worldedit.internal.util.NonAbstractForCompatibility;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

/**
 * Accepts block and entity changes.
 */
public interface OutputExtent {

    /**
     * Change the block at the given location to the given block. The operation may
     * not tie the given {@link BlockStateHolder} to the world, so future changes to the
     * {@link BlockStateHolder} do not affect the world until this method is called again.
     *
     * <p>The return value of this method indicates whether the change was probably
     * successful. It may not be successful if, for example, the location is out
     * of the bounds of the extent. It may be unsuccessful if the block passed
     * is the same as the one in the world. However, the return value is only an
     * estimation and it may be incorrect, but it could be used to count, for
     * example, the approximate number of changes.</p>
     *
     * @param position position of the block
     * @param block    block to set
     * @return true if the block was successfully set (return value may not be accurate)
     * @throws WorldEditException thrown on an error
     * @deprecated It is recommended that you use {@link #setBlock(int, int, int, BlockStateHolder)} in FAWE
     */
    @Deprecated
    default <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return setBlock(position.x(), position.y(), position.z(), block);
    }

    // The defaults need to remain for compatibility (the actual implementation still needs to override one of these)
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return setBlock(MutableBlockVector3.get(x, y, z), block);
    }

    /**
     * @deprecated use {@link #tile(int, int, int, FaweCompoundTag)} instead
     */
    @Deprecated(forRemoval = true, since = "2.11.2")
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return tile(x, y, z, FaweCompoundTag.of(tile.toLinTag()));
    }

    /**
     * Sets a tile/block entity at the given location.
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @param tile the tile/block entity to set
     * @return {@code true} if the tile/block entity was placed
     * @since 2.11.2
     */
    boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException;

    /**
     * Check if this extent fully supports 3D biomes.
     *
     * <p>
     * If {@code false}, the extent only visually reads biomes from {@code y = 0}.
     * The biomes will still be set in 3D, but the client will only see the one at
     * {@code y = 0}. It is up to the caller to determine if they want to set that
     * biome instead, or simply warn the actor.
     * </p>
     *
     * @return if the extent fully supports 3D biomes
     */
    default boolean fullySupports3DBiomes() {
        return true;
    }

    /**
     * Set the biome.
     *
     * @param position the (x, z) location to set the biome at
     * @param biome    the biome to set to
     * @return true if the biome was successfully set (return value may not be accurate)
     * @deprecated Biomes in Minecraft are 3D now, use {@link OutputExtent#setBiome(BlockVector3, BiomeType)}
     */
    @Deprecated
    default boolean setBiome(BlockVector2 position, BiomeType biome) {
        boolean result = false;
        int minY = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMinY();
        int maxY = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMaxY();
        for (int y = minY; y < maxY; y++) {
            result |= setBiome(position.toBlockVector3().mutY(y), biome);
        }
        return result;
    }

    @NonAbstractForCompatibility(
            delegateName = "setBiome",
            delegateParams = {int.class, int.class, int.class, BiomeType.class}
    )
    // The defaults need to remain for compatibility (the actual implementation still needs to override one of these)
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        DeprecationUtil.checkDelegatingOverride(getClass());

        return setBiome(MutableBlockVector3.get(x, y, z), biome);
    }

    //FAWE start

    /**
     * Set the biome.
     *
     * <p>
     * As implementation varies per Minecraft version, this may set more than
     * this position's biome. On versions prior to 1.15, this will set the entire
     * column. On later versions it will set the 4x4x4 cube.
     * </p>
     *
     * @param position the (x, y, z) location to set the biome at
     * @param biome    the biome to set to
     * @return true if the biome was successfully set (return value may not be accurate)
     */
    @NonAbstractForCompatibility(
            delegateName = "setBiome",
            delegateParams = {BlockVector3.class, BiomeType.class}
    )
    default boolean setBiome(BlockVector3 position, BiomeType biome) {
        DeprecationUtil.checkDelegatingOverride(getClass());

        return setBiome(position.toBlockVector2(), biome);
    }

    /**
     * Set the light value.
     *
     * @param position position of the block
     * @param value    light level to set
     */
    default void setBlockLight(BlockVector3 position, int value) {
        setBlockLight(position.x(), position.y(), position.z(), value);
    }

    default void setBlockLight(int x, int y, int z, int value) {
    }

    /**
     * Set the sky light value.
     *
     * @param position position of the block
     * @param value    light level to set
     */
    default void setSkyLight(BlockVector3 position, int value) {
        setSkyLight(position.x(), position.y(), position.z(), value);
    }
    //FAWE end

    default void setSkyLight(int x, int y, int z, int value) {
    }

    default void setHeightMap(HeightMapType type, int[] heightMap) {
    }

    /**
     * Return an {@link Operation} that should be called to tie up loose ends
     * (such as to commit changes in a buffer).
     *
     * @return an operation or null if there is none to execute
     */
    @Nullable
    Operation commit();

}
