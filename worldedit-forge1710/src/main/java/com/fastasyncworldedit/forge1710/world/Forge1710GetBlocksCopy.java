package com.fastasyncworldedit.forge1710.world;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Snapshot of the sections a chunk had before an edit was applied, used by FAWE history (undo).
 */
public class Forge1710GetBlocksCopy extends CharGetBlocks {

    private final int chunkX;
    private final int chunkZ;
    private final char[][] stored = new char[16][];

    Forge1710GetBlocksCopy(int chunkX, int chunkZ) {
        super(0, 15);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    void storeSection(int layer, char[] data) {
        stored[layer] = data;
    }

    @Override
    public boolean hasSection(int layer) {
        return layer >= 0 && layer < 16 && stored[layer] != null;
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            data = new char[4096];
        }
        if (layer >= 0 && layer < 16 && stored[layer] != null) {
            System.arraycopy(stored[layer], 0, data, 0, 4096);
        } else {
            Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
        }
        return data;
    }

    @Override
    public <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalize) {
        throw new UnsupportedOperationException("Cannot apply changes to a history copy");
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return BiomeTypes.PLAINS;
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 15;
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        return new int[256];
    }

    @Override
    public @Nullable FaweCompoundTag entity(UUID uuid) {
        return null;
    }

    @Override
    public Set<Entity> getFullEntities() {
        return Collections.emptySet();
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        return -1;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
    }

    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable FaweCompoundTag tile(int x, int y, int z) {
        return null;
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return Collections.emptyList();
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

}
