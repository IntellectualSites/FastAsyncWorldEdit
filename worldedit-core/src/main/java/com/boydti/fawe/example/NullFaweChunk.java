package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NullFaweChunk extends FaweChunk<Void> {
    public static final NullFaweChunk INSTANCE = new NullFaweChunk(null, 0, 0);

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public NullFaweChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public int[][] getCombinedIdArrays() {
        return new int[16][];
    }

    @Override
    public int[] getIdArray(int layer) {
        return null;
    }

    @Override
    public byte[] getBiomeArray() {
        return new byte[256];
    }

    @Override
    public int getBitMask() {
        return 0;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        return BlockTypes.AIR.getInternalId();
    }

    @Override
    public Void getChunk() {
        return null;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {

    }

    @Override
    public void setEntity(CompoundTag entity) {

    }

    @Override
    public void removeEntity(UUID uuid) {

    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {

    }

    @Override
    public Set<CompoundTag> getEntities() {
        return new HashSet<>();
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return new HashMap<>();
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Override
    public void setBiome(int x, int z, BaseBiome biome) {

    }

    @Override
    public void setBiome(int x, int z, byte biome) {

    }

    @Override
    public FaweChunk<Void> copy(boolean shallow) {
        return this;
    }

    @Override
    public FaweChunk call() {
        return null;
    }
}
