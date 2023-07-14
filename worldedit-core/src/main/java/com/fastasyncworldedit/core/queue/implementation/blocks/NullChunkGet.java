package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
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

    @Nonnull
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Nullable
    public BiomeType getBiomeType(int x, int y, int z) {
        return BiomeTypes.FOREST;
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
    }

    @Nonnull
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable FaweCompoundTag tile(final int x, final int y, final int z) {
        return null;
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return Collections.emptyList();
    }

    @Nullable
    public Set<Entity> getFullEntities() {
        return null;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        return -1;
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
    }

    @Override
    public int getMaxY() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getMaxSectionPosition() {
        return 0;
    }

    @Override
    public int getMinSectionPosition() {
        return 0;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    public boolean trim(boolean aggressive) {
        return true;
    }

    public boolean trim(boolean aggressive, int layer) {
        return true;
    }

    @Nullable
    public <T extends Future<T>> T call(@Nonnull IChunkSet set, @Nonnull Runnable finalize) {
        return null;
    }

    @Override
    public @Nullable FaweCompoundTag entity(final UUID uuid) {
        return null;
    }

    @Nonnull
    public char[] load(int layer) {
        return FaweCache.INSTANCE.EMPTY_CHAR_4096;
    }

    @Nullable
    @Override
    public char[] loadIfPresent(final int layer) {
        return null;
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

    @Nonnull
    public int[] getHeightMap(@Nullable HeightMapType type) {
        return new int[256];
    }

    @Nullable
    public IBlocks reset() {
        return null;
    }

    @Override
    public int getSectionCount() {
        return 0;
    }

    private NullChunkGet() {
    }

}
