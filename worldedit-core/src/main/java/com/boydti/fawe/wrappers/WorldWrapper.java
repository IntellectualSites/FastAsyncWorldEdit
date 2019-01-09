package com.boydti.fawe.wrappers;

import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.weather.WeatherType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class WorldWrapper extends AbstractWorld {

    private final World parent;

    public static WorldWrapper wrap(World world) {
        if (world == null) {
            return null;
        }
        if (world instanceof WorldWrapper) {
            return (WorldWrapper) world;
        }
        return new WorldWrapper(world);
    }

    public static World unwrap(World world) {
        if (world instanceof WorldWrapper) {
            return unwrap(((WorldWrapper) world).getParent());
        }
        else if (world instanceof EditSession) {
            return unwrap(((EditSession) world).getWorld());
        }
        return world;
    }

    private WorldWrapper(World parent) {
        this.parent = parent;
    }

    public World getParent() {
        return parent instanceof WorldWrapper ? ((WorldWrapper) parent).getParent() : parent;
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        return parent.useItem(position, item, face);
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return parent.setBlock(position, block, notifyAndLight);
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public Mask createLiquidMask() {
        return parent.createLiquidMask();
    }

    @Override
    public void dropItem(Vector3 pt, BaseItemStack item, int times) {
        parent.dropItem(pt, item, times);
    }

    @Override
    public void simulateBlockMine(final BlockVector3 pt) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.simulateBlockMine(pt);
            }
        });
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync((Supplier<Boolean>) () -> {
            try {
                return parent.generateTree(type, editSession, position);
            } catch (MaxChangedBlocksException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        parent.checkLoadedChunk(pt);
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
        parent.fixAfterFastMode(chunks);
    }

    @Override
    public void fixLighting(Iterable<BlockVector2> chunks) {
        parent.fixLighting(chunks);
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        return parent.playEffect(position, type, data);
    }

    @Override
    public boolean queueBlockBreakEffect(Platform server, BlockVector3 position, BlockType blockType, double priority) {
        return parent.queueBlockBreakEffect(server, position, blockType, priority);
    }

    @Override
    public WeatherType getWeather() {
        return null;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return parent.getRemainingWeatherDuration();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        parent.setWeather(weatherType);
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        parent.setWeather(weatherType, duration);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return parent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return parent.getMaximumPoint();
    }

    @Override
    @Nullable
    public Operation commit() {
        return parent.commit();
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return parent.getBlockLightLevel(position);
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return parent.clearContainerBlockContents(position);
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        parent.dropItem(position, item);
    }

    @Override
    public boolean regenerate(final Region region, final EditSession session) {
        return session.regenerate(region);
    }

    @Override
    public boolean equals(Object other) {
        return parent.equals(other);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return TaskManager.IMP.sync(new RunnableVal<List<? extends Entity>>() {
            @Override
            public void run(List<? extends Entity> value) {
                this.value = parent.getEntities(region);
            }
        });
    }

    @Override
    public List<? extends Entity> getEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<? extends Entity>>() {
            @Override
            public void run(List<? extends Entity> value) {
                this.value = parent.getEntities();
            }
        });
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return parent.getBlock(position);
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return parent.getLazyBlock(position);
    }

    @Override
    public BlockState getFullBlock(BlockVector3 position) {
        return parent.getFullBlock(position);
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return parent.getBiome(position);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return parent.setBiome(position, biome);
    }
}
