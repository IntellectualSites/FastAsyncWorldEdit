package com.fastasyncworldedit.core.queue.implementation.chunk;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public final class NullChunk implements IQueueChunk {

    private static final NullChunk instance = new NullChunk();

    public static NullChunk getInstance() {
        return instance;
    }

    public int getX() {
        return 0;
    }

    public int getZ() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public Future call() {
        return null;
    }

    public void filterBlocks(@Nonnull Filter filter, @Nonnull ChunkFilterBlock block, @Nullable Region region, boolean full) {
    }

    public boolean setBiome(int x, int y, int z, @Nonnull BiomeType biome) {
        return false;
    }

    public boolean setTile(int x, int y, int z, @Nonnull CompoundTag tag) {
        return false;
    }

    public void setEntity(@Nonnull CompoundTag tag) {
    }

    public void removeEntity(@Nonnull UUID uuid) {
    }

    @Nullable
    public Set<UUID> getEntityRemoves() {
        return null;
    }

    public int getSkyLight(int x, int y, int z) {
        return 15;
    }

    @Nonnull
    public char[][] getLight() {
        return new char[0][];
    }

    @Nonnull
    public char[][] getSkyLight() {
        return new char[0][];
    }

    @Override
    public boolean hasBiomes(final int layer) {
        return false;
    }

    @Nonnull
    public int[] getHeightMap(@Nullable HeightMapType type) {
        return new int[256];
    }

    public int getEmittedLight(int x, int y, int z) {
        return 15;
    }

    public void setSkyLight(int x, int y, int z, int value) {
    }

    public void setHeightMap(@Nullable HeightMapType type, @Nullable int[] heightMap) {
    }

    public boolean fullySupports3DBiomes() {
        return false;
    }

    public void setBlockLight(int x, int y, int z, int value) {
    }

    public void setFullBright(int layer) {
    }

    public void removeSectionLighting(int layer, boolean sky) {
    }

    public void setSkyLightLayer(int layer, @Nullable char[] toSet) {
    }

    public void setLightLayer(int layer, @Nullable char[] toSet) {
    }

    @Nullable
    public BiomeType[] getBiomes() {
        return null;
    }

    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder) {
        return false;
    }

    public void setBlocks(int layer, @Nonnull char[] data) {
    }

    @Nullable
    public BiomeType getBiomeType(int x, int y, int z) {
        return null;
    }

    public boolean hasSection(int layer) {
        return false;
    }

    @Nonnull
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    @Nonnull
    public BaseBlock getFullBlock(int x, int y, int z) {
        return BlockTypes.__RESERVED__.getDefaultState().toBaseBlock();
    }

    @Nonnull
    public Map<BlockVector3, CompoundTag> getTiles() {
        return Collections.emptyMap();
    }

    @Nullable
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Nonnull
    public Set<CompoundTag> getEntities() {
        return Collections.emptySet();
    }

    @Nullable
    public char[] load(int layer) {
        return null;
    }

    @Nullable
    @Override
    public char[] loadIfPresent(final int layer) {
        return null;
    }

    @Nullable
    public CompoundTag getEntity(@Nonnull UUID uuid) {
        return null;
    }

    @Override
    public void setCreateCopy(boolean createCopy) {
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
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

    @Nullable
    public <T extends Future<T>> T call(@Nullable IChunkSet set, @Nullable Runnable finalize) {
        return null;
    }

    public boolean trim(boolean aggressive) {
        return true;
    }

    public boolean trim(boolean aggressive, int layer) {
        return true;
    }

    @Override
    public int getSectionCount() {
        return 0;
    }

    private NullChunk() {
    }

}
