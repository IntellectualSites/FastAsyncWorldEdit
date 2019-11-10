package com.boydti.fawe.regions.general.integrations.plotquared;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IQueueExtent;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;

// TODO FIXME
public class FaweLocalBlockQueue extends LocalBlockQueue {

    public final IQueueExtent IMP;
    private final LegacyMapper legacyMapper;
    private final World world;
    private BlockVector3 mutable = new MutableBlockVector3();

    public FaweLocalBlockQueue(String worldName) {
        super(worldName);
        this.world = FaweAPI.getWorld(worldName);
        IMP = Fawe.get().getQueueHandler().getQueue(world);
        legacyMapper = LegacyMapper.getInstance();
    }

    @Override
    public boolean next() {
        if (!IMP.isEmpty()) {
            IMP.flush();
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
        return IMP.isEmpty() ? 0 : 1;
    }

    @Override
    public void optimize() {
    }

    @Override
    public void setModified(long l) {
    }

    @Override
    public long getModified() {
        return IMP.size();
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState id) {
        return IMP.setBlock(x, y, z, id);
    }

    @Override
    public boolean setBlock(int x, int y, int z, Pattern pattern) {
        mutable.setComponents(x, y, z);
        return pattern.apply(IMP, mutable, mutable);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BaseBlock id) {
        return IMP.setBlock(x, y, z, id);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return IMP.getBlock(x, y, z);
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biomeType) {
        return IMP.setBiome(x, 0, z, biomeType);
    }

    @Override
    public String getWorld() {
        return world.getId();
    }

    @Override
    public void flush() {
        IMP.flush();
    }

    @Override
    public boolean enqueue() {
        boolean val = super.enqueue();
        IMP.enableQueue();
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
        IMP.regenerateChunk(x, z, null, null);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        IMP.setTile(x, y, z, (com.sk89q.jnbt.CompoundTag) FaweCache.IMP.asTag(tag));
        return true;
    }
}
