package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.SimpleIntFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import javax.annotation.Nullable;

public abstract class ImmutableVirtualWorld implements VirtualWorld {
    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        return Collections.emptyList();
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BaseBiome biome, @Nullable Long seed) {
        return unsupported();
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {

    }

    @Override
    public File getSaveFolder() {
        return null;
    }

    @Override
    public void addNotifyTask(int x, int z, Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return FaweCache.getBiome(0);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z, int def) {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public boolean hasSky() {
        return true;
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 15;
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void setWorld(String world) {

    }

    @Override
    public World getWEWorld() {
        return this;
    }

    @Override
    public String getWorldName() {
        return getName();
    }

    @Override
    public long getModified() {
        return 0;
    }

    @Override
    public void setModified(long modified) {
        // Unsupported
    }

    @Override
    public RunnableVal2<ProgressType, Integer> getProgressTask() {
        return null;
    }

    @Override
    public void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask) {

    }

    @Override
    public void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {

    }

    @Override
    public RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return null;
    }

    @Override
    public SetQueue.QueueStage getStage() {
        return SetQueue.QueueStage.NONE;
    }

    @Override
    public void setStage(SetQueue.QueueStage stage) {
        // Not supported
    }

    @Override
    public void addNotifyTask(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void runTasks() {

    }

    @Override
    public void addTask(Runnable whenFree) {
        whenFree.run();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    @Override
    public String getName() {
        return Integer.toString(hashCode());
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return setBlock(position, block);
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return unsupported();
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        unsupported();
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return unsupported();
    }

    @Override
    public FaweChunk getFaweChunk(int chunkX, int chunkZ) {
        return new SimpleIntFaweChunk(this, chunkX, chunkZ);
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        unsupported();
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        unsupported();
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        unsupported();
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        return unsupported();
    }

    @Override
    public void setChunk(FaweChunk chunk) {
        unsupported();
    }

    @Override
    public boolean next(int amount, long time) {
        return unsupported();
    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return unsupported();
    }

    @Override
    public void clear() {
        // do nothing - world is immutable
    }

    private boolean unsupported() {
        throw new UnsupportedOperationException("World is immutable");
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId) {
        return unsupported();
    }

    @Override
    public boolean setBlock(BlockVector3 pt, BlockStateHolder block) throws WorldEditException {
        return unsupported();
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        unsupported();
    }

    @Override
    public WeatherType getWeather() {
        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return 0;
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        unsupported();
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        unsupported();
    }
}
