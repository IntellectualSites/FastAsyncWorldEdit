package com.fastasyncworldedit.core.world;

import com.fastasyncworldedit.core.Fawe;
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
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * An abstract implementation of {@link World}.
 */
public interface SimpleWorld extends World {

    @Override
    default boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        return false;
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, boolean notifyAndLight) throws
            WorldEditException {
        return setBlock(position, block);
    }

    @Override
    default BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
    }

    @Override
    <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 pt, B block) throws WorldEditException;

    @Nullable
    @Override
    default Path getStoragePath() {
        return null;
    }

    @Override
    default int getMaxY() {
        return getMaximumPoint().y();
    }

    @Override
    default int getMinY() {
        return getMinimumPoint().y();
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
        Fawe.instance().getQueueHandler().sync((Supplier<Boolean>) () -> playEffect(
                position,
                2001,
                blockType.getLegacyCombinedId() >> 4
        ));
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
    default boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws
            MaxChangedBlocksException {
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
