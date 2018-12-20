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

package com.sk89q.worldedit.world;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.weather.WeatherType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A null implementation of {@link World} that drops all changes and
 * returns dummy data.
 */
public class NullWorld extends AbstractWorld {

    private static final NullWorld INSTANCE = new NullWorld();

    public NullWorld() {
    }

    @Override
    public String getName() {
        return "null";
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return false;
    }

    @Override
    public int getBlockLightLevel(Vector position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(Vector position) {
        return false;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return null;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Override
    public void dropItem(Vector position, BaseItemStack item) {
    }

    @Override
    public void simulateBlockMine(Vector position) {
    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return false;
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, Vector position) throws MaxChangedBlocksException {
        return false;
    }

    @Override
    public WeatherType getWeather() {
        return null;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return 0;
    }

    @Override
    public void setWeather(WeatherType weatherType) {
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
    }

    @Override
    public BlockState getBlock(Vector position) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BlockState getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public BlockState getFullBlock(Vector position) {
        return getBlock(position);
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

    /**
     * Return an instance of this null world.
     *
     * @return a null world
     */
    public static NullWorld getInstance() {
        return INSTANCE;
    }

}
