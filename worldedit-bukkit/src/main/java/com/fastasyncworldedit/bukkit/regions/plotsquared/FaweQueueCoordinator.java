package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.plotsquared.core.queue.LightingMode;
import com.plotsquared.core.queue.QueueCoordinator;
import com.plotsquared.core.queue.subscriber.ProgressSubscriber;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "rawtypes"})
public class FaweQueueCoordinator extends QueueCoordinator {

    public final IQueueExtent<IQueueChunk> instance;
    private final World world;
    private final BlockVector3 mutable = new MutableBlockVector3();
    private boolean setbiome = false;

    public FaweQueueCoordinator(World world) {
        super(world);
        this.world = world;
        instance = Fawe.instance().getQueueHandler().getQueue(world);
        Fawe.instance().getQueueHandler().unCache();
    }

    @Override
    public int size() {
        return instance.isEmpty() ? 0 : 1;
    }

    @Override
    public void setModified(long l) {
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState id) {
        return instance.setBlock(x, y, z, id);
    }

    @Override
    public boolean setBlock(int x, int y, int z, Pattern pattern) {
        mutable.setComponents(x, y, z);
        return pattern.apply(instance, mutable, mutable);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BaseBlock id) {
        return instance.setBlock(x, y, z, id);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return instance.getBlock(x, y, z);
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biomeType) {
        setbiome = true;
        return instance.setBiome(BlockVector2.at(x, z), biomeType);
    }

    @Override
    public boolean setBiome(int x, int y, int z, @Nonnull BiomeType biome) {
        return false;
    }

    @Override
    public boolean isSettingBiomes() {
        return false;
    }

    @Override
    public boolean setEntity(@Nonnull Entity entity) {
        return false;
    }

    @Nonnull
    @Override
    public List<BlockVector2> getReadChunks() {
        return null;
    }

    @Override
    public void addReadChunks(@Nonnull Set<BlockVector2> readChunks) {

    }

    @Override
    public void addReadChunk(@Nonnull BlockVector2 chunk) {

    }

    @Override
    public boolean isUnloadAfter() {
        return false;
    }

    @Override
    public void setUnloadAfter(boolean unloadAfter) {

    }

    @Nullable
    @Override
    public CuboidRegion getRegenRegion() {
        return null;
    }

    @Override
    public void setRegenRegion(@Nonnull CuboidRegion regenRegion) {

    }

    @Override
    public boolean enqueue() {
        boolean val = super.enqueue();
        instance.enableQueue();
        return val;
    }

    @Override
    public void start() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public Runnable getCompleteTask() {
        return null;
    }

    @Override
    public void setCompleteTask(@Nullable Runnable whenDone) {

    }

    @Nullable
    @Override
    public Consumer<BlockVector2> getChunkConsumer() {
        return null;
    }

    @Override
    public void setChunkConsumer(@Nonnull Consumer<BlockVector2> consumer) {

    }

    @Override
    public void addProgressSubscriber(@Nonnull ProgressSubscriber progressSubscriber) {

    }

    @Nonnull
    @Override
    public LightingMode getLightingMode() {
        return null;
    }

    @Override
    public void setLightingMode(@Nullable LightingMode mode) {

    }

    @Override
    public void regenChunk(int x, int z) {
        instance.regenerateChunk(x, z, null, null);
    }

    @Nullable
    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        instance.setTile(x, y, z, (com.sk89q.jnbt.CompoundTag) FaweCache.INSTANCE.asTag(tag));
        return true;
    }

    @Override
    public boolean isSettingTiles() {
        return false;
    }

}
