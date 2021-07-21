package com.fastasyncworldedit.core.beta.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.beta.IBlocks;
import com.fastasyncworldedit.core.beta.IChunkGet;
import com.fastasyncworldedit.core.beta.IChunkSet;
import com.fastasyncworldedit.core.beta.implementation.lighting.HeightMapType;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public final class NullChunkGet implements IChunkGet {
    private static final NullChunkGet instance = new NullChunkGet();

    public static NullChunkGet getInstance() {
        return instance;
    }

    @NotNull
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Nullable
    public BiomeType getBiomeType(int x, int y, int z) {
        return BiomeTypes.FOREST;
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {}

    @NotNull
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState();
    }

    @NotNull
    public Map<BlockVector3, CompoundTag> getTiles() {
        return Collections.emptyMap();
    }

    @Nullable
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Nullable
    public Set<CompoundTag> getEntities() {
        return null;
    }

    @Nullable
    public CompoundTag getEntity(@NotNull UUID uuid) {
        return null;
    }

    @Override public void setCreateCopy(boolean createCopy) {}

    @Override public boolean isCreateCopy() {
        return false;
    }

    @Override
    public void setLightingToGet(char[][] lighting) {}

    @Override
    public void setSkyLightingToGet(char[][] lighting) {}

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {}

    public boolean trim(boolean aggressive) {
        return true;
    }

    public boolean trim(boolean aggressive, int layer) {
        return true;
    }

    @Nullable
    public <T extends Future<T>> T call(@NotNull IChunkSet set, @NotNull Runnable finalize) {
        return null;
    }

    @NotNull
    public char[] load(int layer) {
        return FaweCache.IMP.EMPTY_CHAR_4096;
    }

    public boolean hasSection(int layer) {
        return false;
    }

    public int getEmittedLight(int x, int y, int z) {
        return 15;
    }

    public int getSkyLight(int x, int y, int z) {
        return 15;
    }

    @NotNull
    public int[] getHeightMap(@Nullable HeightMapType type) {
        return new int[256];
    }

    @Nullable
    public IBlocks reset() {
        return null;
    }

    private NullChunkGet() {
    }

}
