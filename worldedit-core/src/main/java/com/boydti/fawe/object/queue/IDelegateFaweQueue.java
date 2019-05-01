package com.boydti.fawe.object.queue;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.jnbt.anvil.generator.GenBase;
import com.boydti.fawe.jnbt.anvil.generator.Resource;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public interface IDelegateFaweQueue extends FaweQueue {

    FaweQueue getQueue();

    @Override
    default void dequeue() {
        getQueue().dequeue();
    }

    @Override
    default Relighter getRelighter() {
        return getQueue().getRelighter();
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return getQueue().getMinimumPoint();
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return getQueue().getMaximumPoint();
    }

    @Override
    default BlockState getLazyBlock(int x, int y, int z) {
        return getQueue().getLazyBlock(x, y, z);
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return getQueue().setBlock(x, y, z, block);
    }

    @Override
    default BlockState getBlock(BlockVector3 position) {
        return getQueue().getBlock(position);
    }

    @Override
    default BiomeType getBiome(BlockVector2 position) {
        return getQueue().getBiome(position);
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        return getQueue().setBlock(position, block);
    }

    @Override
    default boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getQueue().setBiome(position, biome);
    }

    @Override
    default void addEditSession(EditSession session) {
        getQueue().addEditSession(session);
    }

    @Override
    default void setProgressTracker(RunnableVal2<ProgressType, Integer> progressTask) {
        getQueue().setProgressTracker(progressTask);
    }

    @Override
    default Collection<EditSession> getEditSessions() {
        return getQueue().getEditSessions();
    }

    @Override
    default boolean supports(Capability capability) {
        return getQueue().supports(capability);
    }

    @Override
    default void optimize() {
        getQueue().optimize();
    }

    @Override
    default int setBlocks(CuboidRegion cuboid, int combinedId) {
        return getQueue().setBlocks(cuboid, combinedId);
    }

    @Override
    default boolean setBlock(int x, int y, int z, int combinedId) {
        return getQueue().setBlock(x, y, z, combinedId);
    }

    @Override
    default boolean setBlock(int x, int y, int z, int combinedId, CompoundTag nbt) {
        return getQueue().setBlock(x, y, z, combinedId, nbt);
    }

    @Override
    default void setTile(int x, int y, int z, CompoundTag tag) {
        getQueue().setTile(x, y, z, tag);
    }

    @Override
    default void setEntity(int x, int y, int z, CompoundTag tag) {
        getQueue().setEntity(x, y, z, tag);
    }

    @Override
    default void removeEntity(int x, int y, int z, UUID uuid) {
        getQueue().removeEntity(x, y, z, uuid);
    }

    @Override
    default boolean setBiome(int x, int z, BiomeType biome) {
        return getQueue().setBiome(x, z, biome);
    }

    @Override
    default FaweChunk getFaweChunk(int x, int z) {
        return getQueue().getFaweChunk(x, z);
    }

    @Override
    default Collection<FaweChunk> getFaweChunks() {
        return getQueue().getFaweChunks();
    }

    @Override
    default boolean setMCA(int mcaX, int mcaZ, RegionWrapper region, Runnable whileLocked, boolean save, boolean load) {
        return getQueue().setMCA(mcaX, mcaZ, region, whileLocked, save, load);
    }

    @Override
    default void setChunk(FaweChunk chunk) {
        getQueue().setChunk(chunk);
    }

    @Override
    default File getSaveFolder() {
        return getQueue().getSaveFolder();
    }

    @Override
    default int getMaxY() {
        return getQueue().getMaxY();
    }

    @Override
    default Settings getSettings() {
        return getQueue().getSettings();
    }

    @Override
    default void setSettings(Settings settings) {
        getQueue().setSettings(settings);
    }

    @Override
    default void setWorld(String world) {
        getQueue().setWorld(world);
    }

    @Override
    default World getWEWorld() {
        return getQueue().getWEWorld();
    }

    @Override
    default String getWorldName() {
        return getQueue().getWorldName();
    }

    @Override
    default long getModified() {
        return getQueue().getModified();
    }

    @Override
    default void setModified(long modified) {
        getQueue().setModified(modified);
    }

    @Override
    default RunnableVal2<ProgressType, Integer> getProgressTask() {
        return getQueue().getProgressTask();
    }

    @Override
    default void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask) {
        getQueue().setProgressTask(progressTask);
    }

    @Override
    default void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {
        getQueue().setChangeTask(changeTask);
    }

    @Override
    default RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return getQueue().getChangeTask();
    }

    @Override
    default SetQueue.QueueStage getStage() {
        return getQueue().getStage();
    }

    @Override
    default void setStage(SetQueue.QueueStage stage) {
        getQueue().setStage(stage);
    }

    @Override
    default void addNotifyTask(Runnable runnable) {
        getQueue().addNotifyTask(runnable);
    }

    @Override
    default void runTasks() {
        getQueue().runTasks();
    }

    @Override
    default void addTask(Runnable whenFree) {
        getQueue().addTask(whenFree);
    }

    @Override
    default void forEachBlockInChunk(int cx, int cz, RunnableVal2<BlockVector3, BaseBlock> onEach) {
        getQueue().forEachBlockInChunk(cx, cz, onEach);
    }

    @Override
    default void forEachTileInChunk(int cx, int cz, RunnableVal2<BlockVector3, BaseBlock> onEach) {
        getQueue().forEachTileInChunk(cx, cz, onEach);
    }

    @Override
    @Deprecated
    default boolean regenerateChunk(int x, int z) {
        return getQueue().regenerateChunk(x, z);
    }

    @Override
    default boolean regenerateChunk(int x, int z, @Nullable BiomeType biome, @Nullable Long seed) {
        return getQueue().regenerateChunk(x, z, biome, seed);
    }

    @Override
    default void startSet(boolean parallel) {
        getQueue().startSet(parallel);
    }

    @Override
    default void endSet(boolean parallel) {
        getQueue().endSet(parallel);
    }

    @Override
    default int cancel() {
        return getQueue().cancel();
    }

    @Override
    default void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        getQueue().sendBlockUpdate(chunk, players);
    }

    @Override
    @Deprecated
    default boolean next() {
        return getQueue().next();
    }

    @Override
    default boolean next(int amount, long time) {
        return getQueue().next(amount, time);
    }

    @Override
    default void saveMemory() {
        getQueue().saveMemory();
    }

    @Override
    default void sendChunk(FaweChunk chunk) {
        getQueue().sendChunk(chunk);
    }

    @Override
    default void sendChunk(int x, int z, int bitMask) {
        getQueue().sendChunk(x, z, bitMask);
    }

    @Override
    default void clear() {
        getQueue().clear();
    }

    @Override
    default boolean hasBlock(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getQueue().hasBlock(x, y, z);
    }

    @Override
    default BiomeType getBiomeType(int x, int z) throws FaweException.FaweChunkLoadException {
        return getQueue().getBiomeType(x, z);
    }

    @Override
    default int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getQueue().getCombinedId4Data(x, y, z);
    }

    @Override
    default int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getQueue().getCachedCombinedId4Data(x, y, z);
    }

    @Override
    default int getAdjacentLight(int x, int y, int z) {
        return getQueue().getAdjacentLight(x, y, z);
    }

    @Override
    default boolean hasSky() {
        return getQueue().hasSky();
    }

    @Override
    default int getSkyLight(int x, int y, int z) {
        return getQueue().getSkyLight(x, y, z);
    }

    @Override
    default int getLight(int x, int y, int z) {
        return getQueue().getLight(x, y, z);
    }

    @Override
    default int getEmmittedLight(int x, int y, int z) {
        return getQueue().getEmmittedLight(x, y, z);
    }

    @Override
    default CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getQueue().getTileEntity(x, y, z);
    }

    @Override
    default int getCombinedId4Data(int x, int y, int z, int def) {
        return getQueue().getCombinedId4Data(x, y, z, def);
    }

    @Override
    default int getCachedCombinedId4Data(int x, int y, int z, int def) {
        return getQueue().getCachedCombinedId4Data(x, y, z, def);
    }

    @Override
    default int getCombinedId4DataDebug(int x, int y, int z, int def, EditSession session) {
        return getQueue().getCombinedId4DataDebug(x, y, z, def, session);
    }

    @Override
    default int getBrightness(int x, int y, int z) {
        return getQueue().getBrightness(x, y, z);
    }

    @Override
    default int getOpacityBrightnessPair(int x, int y, int z) {
        return getQueue().getOpacityBrightnessPair(x, y, z);
    }

    @Override
    default int getOpacity(int x, int y, int z) {
        return getQueue().getOpacity(x, y, z);
    }

    @Override
    default int size() {
        return getQueue().size();
    }

    @Override
    default boolean isEmpty() {
        return getQueue().isEmpty();
    }

    @Override
    default void flush() {
        getQueue().flush();
    }

    @Override
    default void flush(int time) {
        getQueue().flush(time);
    }

    @Override
    default boolean enqueue() {
        return getQueue().enqueue();
    }

    @Override
    default List<? extends Entity> getEntities(Region region) {
        return getQueue().getEntities(region);
    }

    @Override
    default List<? extends Entity> getEntities() {
        return getQueue().getEntities();
    }

    @Override
    @Nullable
    default Entity createEntity(Location location, BaseEntity entity) {
        return getQueue().createEntity(location, entity);
    }

    @Override
    default BlockState getLazyBlock(BlockVector3 position) {
        return getQueue().getLazyBlock(position);
    }

    @Nullable
    @Override
    default Operation commit() {
        return getQueue().commit();
    }

    @Override
    default int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return getQueue().getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getQueue().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getQueue().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    default void addCaves(Region region) throws WorldEditException {
        getQueue().addCaves(region);
    }

    @Override
    default void generate(Region region, GenBase gen) throws WorldEditException {
        getQueue().generate(region, gen);
    }

    @Override
    default void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        getQueue().addSchems(region, mask, clipboards, rarity, rotate);
    }

    @Override
    default void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        getQueue().spawnResource(region, gen, rarity, frequency);
    }

    @Override
    default void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        getQueue().addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    default void addOres(Region region, Mask mask) throws WorldEditException {
        getQueue().addOres(region, mask);
    }
}
