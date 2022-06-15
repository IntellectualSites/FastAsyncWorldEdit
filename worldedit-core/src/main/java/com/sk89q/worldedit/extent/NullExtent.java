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

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An extent that returns air blocks for all blocks and does not
 * pass on any changes.
 */
public class NullExtent implements Extent {

    public static final NullExtent INSTANCE = new NullExtent();

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public List<Entity> getEntities(Region region) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    //FAWE start
    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        return null;
    }
    //FAWE end

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return BiomeTypes.THE_VOID;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return false;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return false;
    }

    //FAWE start
    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return false;
    }

    @Override
    public Extent addProcessor(final IBatchProcessor processor) {
        return this;
    }

    @Override
    public Extent addPostProcessor(final IBatchProcessor processor) {
        return this;
    }
    //FAWE end

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

}
