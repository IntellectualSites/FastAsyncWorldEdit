package com.sk89q.worldedit.nukkit;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.nukkit.NukkitNbtConverter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Immutable snapshot copy of a chunk for undo/history.
 */
public class NukkitGetBlocks_Copy implements IChunkGet {

    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;
    private final int minSectionPosition;
    private final int sectionCount;
    private final char[][] blocks;
    private BiomeType[][] biomes;
    private final Map<BlockVector3, FaweCompoundTag> tiles = new HashMap<>();
    private final Set<FaweCompoundTag> entities = new HashSet<>();

    public NukkitGetBlocks_Copy(int chunkX, int chunkZ, int minY, int maxY) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minY = minY;
        this.maxY = maxY;
        this.minSectionPosition = minY >> 4;
        this.sectionCount = (maxY >> 4) - minSectionPosition + 1;
        this.blocks = new char[sectionCount][];
    }

    protected void storeSection(int layer, char[] data) {
        int index = layer - minSectionPosition;
        blocks[index] = data;
    }

    protected void storeBiome(int x, int y, int z, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[sectionCount][];
        }
        int layer = (y >> 4) - minSectionPosition;
        if (biomes[layer] == null) {
            biomes[layer] = new BiomeType[4096];
        }
        int localY = y & 0xF;
        biomes[layer][(localY << 8) | ((z & 0xF) << 4) | (x & 0xF)] = biome;
    }

    protected void storeTile(BlockVector3 pos, FaweCompoundTag tag) {
        tiles.put(pos, tag);
    }

    protected void storeEntity(cn.nukkit.entity.Entity entity) {
        entity.saveNBT();
        entities.add(NukkitNbtConverter.toFawe(entity.namedTag));
    }

    private int layerIndex(int layer) {
        return layer - minSectionPosition;
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = getBlock(x, y, z);
        FaweCompoundTag tileTag = tile(x, y, z);
        if (tileTag != null) {
            LinCompoundTag linTag = tileTag.linTag();
            return state.toBaseBlock(linTag);
        }
        return state.toBaseBlock();
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        if (biomes == null) {
            return BiomeTypes.PLAINS;
        }
        int layer = (y >> 4) - minSectionPosition;
        if (layer < 0 || layer >= sectionCount || biomes[layer] == null) {
            return BiomeTypes.PLAINS;
        }
        int localY = y & 0xF;
        BiomeType type = biomes[layer][(localY << 8) | ((z & 0xF) << 4) | (x & 0xF)];
        return type != null ? type : BiomeTypes.PLAINS;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int layer = y >> 4;
        int index = layerIndex(layer);
        if (index < 0 || index >= sectionCount || blocks[index] == null) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        int localY = y & 0xF;
        char ordinal = blocks[index][((localY << 8) | ((z & 0xF) << 4) | (x & 0xF))];
        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        return BlockTypesCache.states[ordinal];
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

    @Nullable
    @Override
    public FaweCompoundTag tile(int x, int y, int z) {
        return tiles.get(BlockVector3.at(x, y, z));
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return Collections.unmodifiableMap(tiles);
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return this.entities;
    }

    @Override
    public Set<Entity> getFullEntities() {
        throw new UnsupportedOperationException("Cannot get full entities from GET copy.");
    }

    @Nullable
    @Override
    public FaweCompoundTag entity(UUID uuid) {
        for (FaweCompoundTag tag : entities) {
            LinStringTag uuidTag = tag.linTag().findTag("uuid", LinTagType.stringTag());
            if (uuidTag != null) {
                try {
                    if (uuid.equals(UUID.fromString(uuidTag.value()))) {
                        return tag;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public boolean hasSection(int layer) {
        int index = layerIndex(layer);
        return index >= 0 && index < sectionCount && blocks[index] != null;
    }

    @Override
    public char[] load(int layer) {
        int index = layerIndex(layer);
        if (index < 0 || index >= sectionCount) {
            return new char[4096];
        }
        if (blocks[index] == null) {
            return new char[4096];
        }
        return blocks[index];
    }

    @Nullable
    @Override
    public char[] loadIfPresent(int layer) {
        int index = layerIndex(layer);
        if (index < 0 || index >= sectionCount) {
            return null;
        }
        return blocks[index];
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
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
    public int getSectionCount() {
        return sectionCount;
    }

    @Override
    public int getMinSectionPosition() {
        return minSectionPosition;
    }

    @Override
    public int getMaxSectionPosition() {
        return maxY >> 4;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalizer) {
        throw new UnsupportedOperationException("Copy does not support call()");
    }

    @Override
    public IChunkGet reset() {
        return this;
    }

    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        return false;
    }

}
