package com.boydti.fawe.object.queue;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import javax.annotation.Nullable;

public class NullFaweQueue implements FaweQueue {
    private final String worldName;

    public NullFaweQueue(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId) {
        return false;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {

    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {

    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {

    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        return false;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return null;
    }

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        return Collections.emptyList();
    }

    @Override
    public void setChunk(FaweChunk chunk) {

    }

    @Override
    public File getSaveFolder() {
        return null;
    }

    @Override
    public void setWorld(String world) {

    }

    @Override
    public World getWEWorld() {
        return null;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Override
    public long getModified() {
        return 0;
    }

    @Override
    public void setModified(long modified) {

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
    public boolean regenerateChunk(int x, int z, @Nullable BaseBiome biome, @Nullable Long seed) {
        return false;
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {

    }

    @Override
    public boolean next(int amount, long time) {
        return false;
    }

    @Override
    public void sendChunk(FaweChunk chunk) {

    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void addNotifyTask(int x, int z, Runnable runnable) {
        runnable.run();
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        return 0;
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return 0;
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return 0;
    }

    @Override
    public boolean hasSky() {
        return true;
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 0;
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
}
