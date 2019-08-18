package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.concurrent.Future;

public enum NullChunkGet implements IChunkGet {
    INSTANCE
    ;
    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return BiomeTypes.FOREST;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        return null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        return true;
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalize) {
        return null;
    }

    @Override
    public char[] load(int layer) {
        return FaweCache.IMP.EMPTY_CHAR_4096;
    }

    @Override
    public boolean hasSection(int layer) {
        return false;
    }

    @Override
    public IBlocks reset() {
        return null;
    }
}
