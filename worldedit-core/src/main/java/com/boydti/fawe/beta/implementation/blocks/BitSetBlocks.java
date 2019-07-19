package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.collection.BitArray4096;
import com.boydti.fawe.object.collection.BlockSet;
import com.boydti.fawe.object.collection.MemBlockSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BitSetBlocks implements IChunkSet {
    private final MemBlockSet.RowZ row;
    private final BlockState blockState;

    public BitSetBlocks(BlockState blockState) {
        this.row = new MemBlockSet.RowZ();
        this.blockState = blockState;
    }


    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder holder) {
        row.set(null, x, y, z);
        return true;
    }

    @Override
    public boolean isEmpty() {
        return row.isEmpty();
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {}

    @Override
    public void setEntity(CompoundTag tag) {}

    @Override
    public void removeEntity(UUID uuid) {}

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (row.get(null, x, y, z)) {
            return blockState;
        }
        return null;
    }

    @Override
    public char[] getArray(int layer) {

    }

    @Override
    public BiomeType[] getBiomes() {
        return null;
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return null;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return null;
    }

    @Override
    public boolean hasSection(int layer) {
        return false;
    }

    @Override
    public IChunkSet reset() {
        row.reset();
        return this;
    }

    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }
}
