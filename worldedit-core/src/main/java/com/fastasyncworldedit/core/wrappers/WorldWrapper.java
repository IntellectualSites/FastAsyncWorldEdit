package com.fastasyncworldedit.core.wrappers;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.generation.ConfiguredFeatureType;
import com.sk89q.worldedit.world.generation.StructureType;
import com.sk89q.worldedit.world.weather.WeatherType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WorldWrapper extends AbstractWorld {

    private final World parent;

    private WorldWrapper(World parent) {
        this.parent = parent;
    }

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
        } else if (world instanceof EditSession) {
            return unwrap(((EditSession) world).getWorld());
        }
        return world;
    }

    public static World unwrap(Extent extent) {
        if (extent.isWorld()) {
            if (extent instanceof World) {
                return unwrap((World) extent);
            }
            if (extent instanceof AbstractDelegateExtent) {
                return unwrap(new ExtentTraverser<>(extent).find(World.class).get());
            }
        }
        return null;
    }

    public World getParent() {
        return parent instanceof WorldWrapper ? ((WorldWrapper) parent).getParent() : parent;
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        return parent.useItem(position, item, face);
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public int getMinY() {
        return parent.getMinY();
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
    public boolean playBlockBreakEffect(Vector3 position, BlockType type) {
        return parent.playBlockBreakEffect(position, type);
    }

    @Override
    public boolean queueBlockBreakEffect(Platform server, BlockVector3 position, BlockType blockType, double priority) {
        return parent.queueBlockBreakEffect(server, position, blockType, priority);
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
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException {
        return parent.tile(x, y, z, tile);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return parent.setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return parent.setBiome(position, biome);
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public String getNameUnsafe() {
        return parent.getNameUnsafe();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, boolean notifyAndLight) throws
            WorldEditException {
        return parent.setBlock(position, block, notifyAndLight);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) throws
            WorldEditException {
        return parent.setBlock(position, block, sideEffects);
    }

    @Override
    public boolean notifyAndLightBlock(BlockVector3 position, BlockState previousType) throws WorldEditException {
        return parent.notifyAndLightBlock(position, previousType);
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet)
            throws WorldEditException {
        return parent.applySideEffects(position, previousType, sideEffectSet);
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
    public void simulateBlockMine(BlockVector3 pt) {
        TaskManager.taskManager().sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.simulateBlockMine(pt);
            }
        });
    }

    @Override
    public Collection<BaseItemStack> getBlockDrops(final BlockVector3 position) {
        return TaskManager.taskManager().sync(() -> parent.getBlockDrops(position));
    }

    @Override
    public boolean canPlaceAt(final BlockVector3 position, final BlockState blockState) {
        return parent.canPlaceAt(position, blockState);
    }

    @Override
    public boolean regenerate(Region region, EditSession session) {
        return parent.regenerate(region, session);
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        return parent.regenerate(region, extent, options);
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws
            MaxChangedBlocksException {
        try {
            return parent.generateTree(type, editSession, position);
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean generateStructure(final StructureType type, final EditSession editSession, final BlockVector3 position) {
        return parent.generateStructure(type, editSession, position);
    }

    @Override
    public boolean generateFeature(final ConfiguredFeatureType type, final EditSession editSession, final BlockVector3 position) {
        return parent.generateFeature(type, editSession, position);
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return parent.getSpawnPosition();
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        parent.refreshChunk(chunkX, chunkZ);
    }

    @Override
    public IChunkGet get(int x, int z) {
        return parent.get(x, z);
    }

    @Override
    public void sendFakeChunk(@Nullable Player player, ChunkPacket packet) {
        parent.sendFakeChunk(player, packet);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return parent.equals(other);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return parent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return parent.getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        return parent.createEntity(location, entity, uuid);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return parent.getBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return parent.getFullBlock(position);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return parent.getBiome(position);
    }

    @Override
    public void flush() {
        parent.flush();
    }

}
