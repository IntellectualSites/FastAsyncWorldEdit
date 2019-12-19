package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

public enum NullChunk implements IQueueChunk {
    INSTANCE;

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Future call() {
        return null;
    }

    @Override
    public void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, boolean full) {

    }

//    @Override
//    public void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
//
//    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        return false;
    }

    @Override
    public void setEntity(CompoundTag tag) {

    }

    @Override
    public void removeEntity(UUID uuid) {

    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return null;
    }

    @Override
    public BiomeType[] getBiomes() {
        return null;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) {
        return false;
    }

    @Override
    public void setBlocks(int layer, char[] data) {

    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return null;
    }

    @Override
    public boolean hasSection(int layer) {
        return false;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypes.__RESERVED__.getDefaultState().toBaseBlock();
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        return null;
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return Collections.emptyMap();
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return Collections.emptySet();
    }

    @Override
    public char[] load(int layer) {
        return null;
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        return null;
    }

    @Override
    public Future call(IChunkSet set, Runnable finalize) {
        return null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        return true;
    }
}

