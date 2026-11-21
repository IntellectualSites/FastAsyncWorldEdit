package com.fastasyncworldedit.nukkit.blocks;

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
 * <p>
 * Because Nukkit does not expose chunk sections as first-class objects,
 * this copy stores block data in char arrays indexed by section position.
 * The snapshot is taken before modifications in
 * {@link NukkitGetBlocks#call} and retrieved later for undo operations.
 * <p>
 * Unlike Bukkit's copy implementations, which can delegate to NMS chunk
 * snapshot methods, this class must manually iterate and store every
 * block, tile entity, biome, and entity because Nukkit's API does not
 * provide a native snapshot mechanism.
 * <p>
 * Lighting and heightmap data are not stored because Nukkit recalculates
 * them automatically on block changes. Tile entities and entities are
 * stored as FAWE compound tags for cross-platform compatibility.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Manual per-coordinate block storage instead of section snapshots</li>
 *   <li>No lighting or heightmap capture; Nukkit handles these internally</li>
 *   <li>Entity UUIDs must be injected into NBT because NKX does not save them</li>
 * </ul>
 *
 * @see NukkitGetBlocks
 */
public class NukkitGetBlocks_Copy implements IChunkGet {

    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;
    private final int minSectionPosition;
    private final int sectionCount;
    private final char[][] blocks;
    private final Map<BlockVector3, FaweCompoundTag> tiles = new HashMap<>();
    private final Set<FaweCompoundTag> entities = new HashSet<>();
    private BiomeType[][] biomes;

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

    protected void storeEntity(cn.nukkit.entity.Entity entity, UUID entityUUID) {
        entity.saveNBT();
        // Ensure UUID is stored in NBT (NKX entities don't save it by default)
        if (!entity.namedTag.contains("uuid")) {
            entity.namedTag.putString("uuid", entityUUID.toString());
        }
        entities.add(NukkitNbtConverter.toFawe(entity.namedTag));
    }

    private int normalizedHeight(int y) {
        return y - (getMinSectionPosition() << 4) + 1;
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
        int[] heightMap = new int[256];
        int found = 0;
        for (int y = maxY; y >= minY; y--) {
            int layer = y >> 4;
            int index = layerIndex(layer);
            if (index < 0 || index >= sectionCount || blocks[index] == null) {
                continue;
            }
            int localY = y & 0xF;
            char[] section = blocks[index];
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int heightIndex = (z << 4) | x;
                    if (heightMap[heightIndex] != 0) {
                        continue;
                    }
                    char ordinal = section[(localY << 8) | (z << 4) | x];
                    if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                        continue;
                    }
                    BlockState state = BlockTypesCache.states[ordinal];
                    if (state != null && type.includes(state)) {
                        heightMap[heightIndex] = normalizedHeight(y);
                        if (++found == 256) {
                            return heightMap;
                        }
                    }
                }
            }
        }
        return heightMap;
    }

    @Nullable
    @Override
    public FaweCompoundTag tile(int x, int y, int z) {
        FaweCompoundTag tag = tiles.get(BlockVector3.at(x, y, z));
        if (tag != null) {
            return tag;
        }
        return tiles.get(BlockVector3.at((chunkX << 4) + (x & 0xF), y, (chunkZ << 4) + (z & 0xF)));
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return Collections.unmodifiableMap(tiles);
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return Collections.unmodifiableSet(entities);
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
        // Nukkit recalculates heightmaps; snapshot copies only expose computed read data.
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
        // A snapshot copy is never applied via call() (the primary GET handles that); this method
        // is only reached if a caller treats the copy uniformly. Return null to match the Bukkit
        // sibling convention rather than throwing, so uniform IChunkGet handling degrades gracefully.
        return null;
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
