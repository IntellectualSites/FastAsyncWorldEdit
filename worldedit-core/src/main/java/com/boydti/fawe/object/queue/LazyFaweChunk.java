package com.boydti.fawe.object.queue;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.sk89q.jnbt.CompoundTag;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public abstract class LazyFaweChunk<T extends FaweChunk> extends FaweChunk {

    private T parent;

    public LazyFaweChunk(FaweQueue queue, int chunkX, int chunkZ) {
        super(queue, chunkX, chunkZ);
    }

    private T internalGetOrCacheChunk() {
        T tmp = parent;
        if (tmp == null) parent = tmp = getChunk();
        return tmp;
    }

    protected T getCachedChunk() {
        return parent;
    }
    
    public abstract T getChunk();

    @Override
    public FaweQueue getParent() {
        return internalGetOrCacheChunk().getParent();
    }

    @Override
    public long longHash() {
        return internalGetOrCacheChunk().longHash();
    }

    @Override
    public int hashCode() {
        return internalGetOrCacheChunk().hashCode();
    }

    @Override
    public void addToQueue() {
        internalGetOrCacheChunk().addToQueue();
    }

    @Override
    public int getBitMask() {
        return internalGetOrCacheChunk().getBitMask();
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        return internalGetOrCacheChunk().getBlockCombinedId(x, y, z);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockStateHolder block) {
        internalGetOrCacheChunk().setBlock(x, y, z, block);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return internalGetOrCacheChunk().getBlock(x, y, z);
    }

    @Override
    @Nullable
    public int[] getIdArray(int layer) {
        return internalGetOrCacheChunk().getIdArray(layer);
    }

    @Override
    public byte[][] getBlockLightArray() {
        return internalGetOrCacheChunk().getBlockLightArray();
    }

    @Override
    public byte[][] getSkyLightArray() {
        return internalGetOrCacheChunk().getSkyLightArray();
    }

    @Override
    public BiomeType[] getBiomeArray() {
        return internalGetOrCacheChunk().getBiomeArray();
    }

    @Override
    public void forEachQueuedBlock(FaweChunkVisitor onEach) {
        internalGetOrCacheChunk().forEachQueuedBlock(onEach);
    }

    @Override
    public int[][] getCombinedIdArrays() {
        return internalGetOrCacheChunk().getCombinedIdArrays();
    }

    @Override
    public void fill(int combinedId) {
        internalGetOrCacheChunk().fill(combinedId);
    }

    @Override
    public void fillCuboid(int x1, int x2, int y1, int y2, int z1, int z2, int combinedId) {
        internalGetOrCacheChunk().fillCuboid(x1, x2, y1, y2, z1, z2, combinedId);
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        internalGetOrCacheChunk().setTile(x, y, z, tile);
    }

    @Override
    public void setEntity(CompoundTag entity) {
        internalGetOrCacheChunk().setEntity(entity);
    }

    @Override
    public void removeEntity(UUID uuid) {
        internalGetOrCacheChunk().removeEntity(uuid);
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        internalGetOrCacheChunk().setBlock(x, y, z, combinedId);
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return internalGetOrCacheChunk().getEntities();
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return internalGetOrCacheChunk().getEntityRemoves();
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return internalGetOrCacheChunk().getTiles();
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return internalGetOrCacheChunk().getTile(x, y, z);
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        internalGetOrCacheChunk().setBiome(x, z, biome);
    }

    @Override
    public void setBiome(BiomeType biome) {
        internalGetOrCacheChunk().setBiome(biome);
    }

    @Override
    public void optimize() {
        internalGetOrCacheChunk().optimize();
    }

    @Override
    public boolean equals(Object obj) {
        return internalGetOrCacheChunk().equals(obj);
    }

    @Override
    public FaweChunk copy(boolean shallow) {
        return internalGetOrCacheChunk().copy(shallow);
    }

    @Override
    public void start() {
        internalGetOrCacheChunk().start();
    }

    @Override
    public void end() {
        internalGetOrCacheChunk().end();
    }

    @Override
    public FaweChunk call() {
        return internalGetOrCacheChunk().call();
    }
}
