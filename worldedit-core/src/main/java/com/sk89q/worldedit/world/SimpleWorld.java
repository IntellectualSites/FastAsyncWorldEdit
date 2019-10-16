/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General default License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General default License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General default License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.world;

import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import java.nio.file.Path;
import javax.annotation.Nullable;

/**
 * An abstract implementation of {@link World}.
 * Added by FAWE.
 */
public interface SimpleWorld extends World {
    @Override
    default boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        return false;
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, boolean notifyAndLight) throws WorldEditException {
        return setBlock(position, block);
    }

    @Override
    default BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
    }

    @Override
    <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 pt, B block) throws WorldEditException;

    @Nullable @Override default Path getStoragePath() {
        return null;
    }

    @Override
    default int getMaxY() {
        return getMaximumPoint().getBlockY();
    }

    @Override
    default Mask createLiquidMask() {
        return new BlockMask(this).add(BlockTypes.LAVA, BlockTypes.WATER);
    }

    @Override
    default void dropItem(Vector3 pt, BaseItemStack item, int times) {
        for (int i = 0; i < times; ++i) {
            dropItem(pt, item);
        }
    }

    @Override
    default void checkLoadedChunk(BlockVector3 pt) {
    }

    @Override
    default void fixAfterFastMode(Iterable<BlockVector2> chunks) {
    }

    @Override
    default void fixLighting(Iterable<BlockVector2> chunks) {
    }

//    @Override
    default boolean playEffect(BlockVector3 position, int type, int data) {
        return false;
    }

    @Override
    default boolean queueBlockBreakEffect(Platform server, BlockVector3 position, BlockType blockType, double priority) {
        SetQueue.IMP.addTask(() -> playEffect(position, 2001, blockType.getLegacyCombinedId() >> 4));
        return true;
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return BlockVector3.at(-30000000, 0, -30000000);
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return BlockVector3.at(30000000, 255, 30000000);
    }


    @Override
    default boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return false;
    }

    @Override
    default void simulateBlockMine(BlockVector3 position) {
        try {
            setBlock(position, BlockTypes.AIR.getDefaultState());
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    default WeatherType getWeather() {
        return WeatherTypes.CLEAR;
    }

    @Override
    default long getRemainingWeatherDuration() {
        return 0;
    }

    @Override
    default void setWeather(WeatherType weatherType) {

    }

    @Override
    default void setWeather(WeatherType weatherType, long duration) {

    }
}
