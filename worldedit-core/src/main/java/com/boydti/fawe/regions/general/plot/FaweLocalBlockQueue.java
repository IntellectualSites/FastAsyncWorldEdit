package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.github.intellectualsites.plotsquared.plot.object.PlotBlock;
import com.github.intellectualsites.plotsquared.plot.util.StringMan;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.util.Collection;
import java.util.List;

// TODO FIXME
public class FaweLocalBlockQueue extends LocalBlockQueue {

    public final FaweQueue IMP;
    private final LegacyMapper legacyMapper;

    public FaweLocalBlockQueue(String world) {
        super(world);
        IMP = SetQueue.IMP.getNewQueue(FaweAPI.getWorld(world), true, false);
        legacyMapper = LegacyMapper.getInstance();
    }

    @Override
    public boolean next() {
        return IMP.size() > 0;
    }

    @Override
    public void startSet(boolean parallel) {
        IMP.startSet(parallel);
    }

    @Override
    public void endSet(boolean parallel) {
        IMP.endSet(parallel);
    }

    @Override
    public int size() {
        return IMP.size();
    }

    @Override
    public void optimize() {
        IMP.optimize();
    }

    @Override
    public void setModified(long l) {
        IMP.setModified(l);
    }

    @Override
    public long getModified() {
        return IMP.getModified();
    }
    
    @Override
    public boolean setBlock(final int x, final int y, final int z, final PlotBlock id) {
    	return setBlock(x, y, z, legacyMapper.getBaseBlockFromPlotBlock(id));
    }
    
    @Override
    public boolean setBlock(final int x, final int y, final int z, final BaseBlock id) {
    	return IMP.setBlock(x, y, z, id);
    }

    @Override
    public PlotBlock getBlock(int x, int y, int z) {
        int combined = IMP.getCombinedId4Data(x, y, z);
        com.sk89q.worldedit.world.block.BlockState state = com.sk89q.worldedit.world.block.BlockState.getFromInternalId(combined);
        return PlotBlock.get(state.getInternalBlockTypeId(), state.getInternalPropertiesId());
    }

    private BiomeType biome;
    private String lastBiome;
    private BiomeRegistry reg;

    @Override
    public boolean setBiome(int x, int z, String biome) {
        if (!StringMan.isEqual(biome, lastBiome)) {
            if (reg == null) {
                reg = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.USER_COMMANDS).getRegistries().getBiomeRegistry();
            }
            Collection<BiomeType> biomes = BiomeTypes.values();
            lastBiome = biome;
            this.biome = Biomes.findBiomeByName(biomes, biome, reg);
        }
        return IMP.setBiome(x, z, this.biome);
    }

    @Override
    public String getWorld() {
        return IMP.getWorldName();
    }

    @Override
    public void flush() {
        IMP.flush();
    }

    @Override
    public void enqueue() {
        super.enqueue();
        IMP.enqueue();
    }

    @Override
    public void refreshChunk(int x, int z) {
        IMP.sendChunk(IMP.getFaweChunk(x, z));
    }

    @Override
    public void fixChunkLighting(int x, int z) {
    }

    @Override
    public void regenChunk(int x, int z) {
        IMP.regenerateChunk(x, z);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        IMP.setTile(x, y, z, (com.sk89q.jnbt.CompoundTag) FaweCache.asTag(tag));
        return true;
    }
}
