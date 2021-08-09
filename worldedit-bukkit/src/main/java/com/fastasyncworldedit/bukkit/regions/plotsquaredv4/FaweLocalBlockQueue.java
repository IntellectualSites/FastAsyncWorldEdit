package com.fastasyncworldedit.bukkit.regions.plotsquaredv4;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

// TODO FIXME
public class FaweLocalBlockQueue extends LocalBlockQueue {

    public final IQueueExtent<IQueueChunk> instance;
    private final World world;
    private final BlockVector3 mutable = new MutableBlockVector3();

    public FaweLocalBlockQueue(String worldName) {
        super(worldName);
        this.world = FaweAPI.getWorld(worldName);
        instance = Fawe.get().getQueueHandler().getQueue(world);
        Fawe.get().getQueueHandler().unCache();
    }

    @Override
    public boolean next() {
        if (!instance.isEmpty()) {
            instance.flush();
        }
        return false;
    }

    @Override
    public void startSet(boolean parallel) {
        Fawe.get().getQueueHandler().startSet(parallel);
    }

    @Override
    public void endSet(boolean parallel) {
        Fawe.get().getQueueHandler().endSet(parallel);
    }

    @Override
    public int size() {
        return instance.isEmpty() ? 0 : 1;
    }

    @Override
    public void optimize() {
    }

    @Override
    public void setModified(long l) {
    }

    @Override
    public long getModified() {
        return instance.size();
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
        return instance.setBiome(x, 0, z, biomeType);
    }

    @Override
    public String getWorld() {
        return world.getId();
    }

    @Override
    public void flush() {
        instance.flush();
    }

    @Override
    public boolean enqueue() {
        boolean val = super.enqueue();
        instance.enableQueue();
        return val;
    }

    @Override
    public void refreshChunk(int x, int z) {
        world.refreshChunk(x, z);
    }

    @Override
    public void fixChunkLighting(int x, int z) {
    }

    @Override
    public void regenChunk(int x, int z) {
        instance.regenerateChunk(x, z, null, null);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        instance.setTile(x, y, z, (com.sk89q.jnbt.CompoundTag) FaweCache.IMP.asTag(tag));
        return true;
    }

}
