package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Equivalent to {@link CharSetBlocks} without any attempt to make thread-safe for improved performance.
 * This is currently only used as a "copy" of {@link CharSetBlocks} to provide to
 * {@link com.fastasyncworldedit.core.queue.IBatchProcessor} instances for processing without overlapping the continuing edit.
 *
 * @since 2.6.2
 */
public class ThreadUnsafeCharBlocks implements IChunkSet, IBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final char defaultOrdinal;
    private char[][] blocks;
    private int minSectionPosition;
    private int maxSectionPosition;
    private int sectionCount;
    private BiomeType[][] biomes;
    private char[][] light;
    private char[][] skyLight;
    private BlockVector3ChunkMap<CompoundTag> tiles;
    private HashSet<CompoundTag> entities;
    private HashSet<UUID> entityRemoves;
    private Map<HeightMapType, int[]> heightMaps;
    private boolean fastMode;
    private int bitMask;

    /**
     * New instance given the data stored in a {@link CharSetBlocks} instance.
     *
     * @since 2.6.2
     */
    ThreadUnsafeCharBlocks(
            char[][] blocks,
            int minSectionPosition,
            int maxSectionPosition,
            BiomeType[][] biomes,
            int sectionCount,
            char[][] light,
            char[][] skyLight,
            BlockVector3ChunkMap<CompoundTag> tiles,
            HashSet<CompoundTag> entities,
            HashSet<UUID> entityRemoves,
            Map<HeightMapType, int[]> heightMaps,
            char defaultOrdinal,
            boolean fastMode,
            int bitMask
    ) {
        this.blocks = blocks;
        this.minSectionPosition = minSectionPosition;
        this.maxSectionPosition = maxSectionPosition;
        this.biomes = biomes;
        this.sectionCount = sectionCount;
        this.light = light;
        this.skyLight = skyLight;
        this.tiles = tiles;
        this.entities = entities;
        this.entityRemoves = entityRemoves;
        this.heightMaps = heightMaps;
        this.defaultOrdinal = defaultOrdinal;
        this.fastMode = fastMode;
        this.bitMask = bitMask;
    }

    @Override
    public boolean hasSection(int layer) {
        layer -= minSectionPosition;
        return layer >= 0 && layer < blocks.length && blocks[layer] != null && blocks[layer].length == FaweCache.INSTANCE.BLOCKS_PER_LAYER;
    }

    @Override
    public char[] load(int layer) {
        updateSectionIndexRange(layer);
        layer -= minSectionPosition;
        char[] arr = blocks[layer];
        if (arr == null) {
            arr = blocks[layer] = new char[FaweCache.INSTANCE.BLOCKS_PER_LAYER];
        }
        return arr;
    }

    @Nullable
    @Override
    public char[] loadIfPresent(int layer) {
        if (layer < minSectionPosition || layer > maxSectionPosition) {
            return null;
        }
        layer -= minSectionPosition;
        return blocks[layer];
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return tiles == null ? Collections.emptyMap() : tiles;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return tiles == null ? null : tiles.get(x, y, z);
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities == null ? Collections.emptySet() : entities;
    }

    @Override
    public Map<HeightMapType, int[]> getHeightMaps() {
        return heightMaps == null ? new HashMap<>() : heightMaps;
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
        updateSectionIndexRange(layer);
        layer -= minSectionPosition;
        if (light == null) {
            light = new char[sectionCount][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 0);
        if (sky) {
            if (skyLight == null) {
                skyLight = new char[sectionCount][];
            }
            if (skyLight[layer] == null) {
                skyLight[layer] = new char[4096];
            }
            Arrays.fill(skyLight[layer], (char) 0);
        }
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        return false;
    }

    @Override
    public int getSectionCount() {
        return sectionCount;
    }

    @Override
    public int getMaxSectionPosition() {
        return maxSectionPosition;
    }

    @Override
    public int getMinSectionPosition() {
        return minSectionPosition;
    }

    public char get(int x, int y, int z) {
        int layer = (y >> 4);
        if (!hasSection(layer)) {
            return defaultOrdinal;
        }
        final int index = (y & 15) << 8 | z << 4 | x;
        return blocks[layer - minSectionPosition][index];
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        int layer;
        if (biomes == null || (y >> 4) < minSectionPosition || (y >> 4) > maxSectionPosition) {
            return null;
        } else if (biomes[(layer = (y >> 4) - minSectionPosition)] == null) {
            return null;
        }
        return biomes[layer][(y & 15) >> 2 | (z >> 2) << 2 | x >> 2];
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        updateSectionIndexRange(y >> 4);
        int layer = (y >> 4) - minSectionPosition;
        if (biomes == null) {
            biomes = new BiomeType[sectionCount][];
            biomes[layer] = new BiomeType[64];
        } else if (biomes[layer] == null) {
            biomes[layer] = new BiomeType[64];
        }
        biomes[layer][(y & 12) << 2 | (z & 12) | (x & 12) >> 2] = biome;
        return true;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
    }

    public void set(int x, int y, int z, char value) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        try {
            blocks[layer][index] = value;
        } catch (ArrayIndexOutOfBoundsException exception) {
            LOGGER.error("Tried setting block at coordinates (" + x + "," + y + "," + z + ")");
            assert Fawe.platform() != null;
            LOGGER.error("Layer variable was = {}", layer, exception);
        }
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder) {
        updateSectionIndexRange(y >> 4);
        set(x, y, z, holder.getOrdinalChar());
        holder.applyTileEntity(this, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        updateSectionIndexRange(layer);
        layer -= minSectionPosition;
        this.blocks[layer] = data;
    }

    @Override
    public boolean isEmpty() {
        if (biomes != null
                || light != null
                || skyLight != null
                || (entities != null && !entities.isEmpty())
                || (tiles != null && !tiles.isEmpty())
                || (entityRemoves != null && !entityRemoves.isEmpty())
                || (heightMaps != null && !heightMaps.isEmpty())) {
            return false;
        }
        for (int i =  minSectionPosition; i <= maxSectionPosition; i++) {
            if (hasSection(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        updateSectionIndexRange(y >> 4);
        if (tiles == null) {
            tiles = new BlockVector3ChunkMap<>();
        }
        tiles.put(x, y, z, tile);
        return true;
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
        updateSectionIndexRange(y >> 4);
        if (light == null) {
            light = new char[sectionCount][];
        }
        final int layer = (y >> 4) - minSectionPosition;
        if (light[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            light[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        light[layer][index] = (char) value;
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
        updateSectionIndexRange(y >> 4);
        if (skyLight == null) {
            skyLight = new char[sectionCount][];
        }
        final int layer = (y >> 4) - minSectionPosition;
        if (skyLight[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            skyLight[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        skyLight[layer][index] = (char) value;
    }

    @Override
    public void setHeightMap(HeightMapType type, int[] heightMap) {
        if (heightMaps == null) {
            heightMaps = new EnumMap<>(HeightMapType.class);
        }
        heightMaps.put(type, heightMap);
    }

    @Override
    public void setLightLayer(int layer, char[] toSet) {
        updateSectionIndexRange(layer);
        if (light == null) {
            light = new char[sectionCount][];
        }
        layer -= minSectionPosition;
        light[layer] = toSet;
    }

    @Override
    public void setSkyLightLayer(int layer, char[] toSet) {
        updateSectionIndexRange(layer);
        if (skyLight == null) {
            skyLight = new char[sectionCount][];
        }
        layer -= minSectionPosition;
        skyLight[layer] = toSet;
    }

    @Override
    public void setFullBright(int layer) {
        updateSectionIndexRange(layer);
        layer -= minSectionPosition;
        if (light == null) {
            light = new char[sectionCount][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        if (skyLight == null) {
            skyLight = new char[sectionCount][];
        }
        if (skyLight[layer] == null) {
            skyLight[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 15);
        Arrays.fill(skyLight[layer], (char) 15);
    }

    @Override
    public void setEntity(CompoundTag tag) {
        if (entities == null) {
            entities = new HashSet<>();
        }
        entities.add(tag);
    }

    @Override
    public void removeEntity(UUID uuid) {
        if (entityRemoves == null) {
            entityRemoves = new HashSet<>();
        }
        entityRemoves.add(uuid);
    }

    @Override
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    @Override
    public boolean isFastMode() {
        return fastMode;
    }

    @Override
    public void setBitMask(int bitMask) {
        this.bitMask = bitMask;
    }

    @Override
    public int getBitMask() {
        return bitMask;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return entityRemoves == null ? Collections.emptySet() : entityRemoves;
    }

    @Override
    public BiomeType[][] getBiomes() {
        return biomes;
    }

    @Override
    public boolean hasBiomes() {
        return IChunkSet.super.hasBiomes();
    }

    @Override
    public char[][] getLight() {
        return light;
    }

    @Override
    public char[][] getSkyLight() {
        return skyLight;
    }

    @Override
    public boolean hasLight() {
        return IChunkSet.super.hasLight();
    }

    @Override
    public IChunkSet reset() {
        blocks = new char[sectionCount][];
        biomes = new BiomeType[sectionCount][];
        light = new char[sectionCount][];
        skyLight = new char[sectionCount][];
        tiles.clear();
        entities.clear();
        entityRemoves.clear();
        heightMaps.clear();
        return this;
    }

    @Override
    public boolean hasBiomes(int layer) {
        layer -= minSectionPosition;
        return layer >= 0 && layer < biomes.length && biomes[layer] != null && biomes[layer].length > 0;
    }

    @Override
    public IChunkSet createCopy() {
        char[][] blocksCopy = new char[sectionCount][];
        for (int i = 0; i < sectionCount; i++) {
            blocksCopy[i] = new char[FaweCache.INSTANCE.BLOCKS_PER_LAYER];
            if (blocks[i] != null) {
                System.arraycopy(blocks[i], 0, blocksCopy[i], 0, FaweCache.INSTANCE.BLOCKS_PER_LAYER);
            }
        }
        BiomeType[][] biomesCopy;
        if (biomes == null) {
            biomesCopy = null;
        } else {
            biomesCopy = new BiomeType[sectionCount][];
            for (int i = 0; i < sectionCount; i++) {
                if (biomes[i] != null) {
                    biomesCopy[i] = new BiomeType[biomes[i].length];
                    System.arraycopy(biomes[i], 0, biomesCopy[i], 0, biomes[i].length);
                }
            }
        }
        char[][] lightCopy = CharSetBlocks.createLightCopy(light, sectionCount);
        char[][] skyLightCopy = CharSetBlocks.createLightCopy(skyLight, sectionCount);
        return new ThreadUnsafeCharBlocks(
                blocksCopy,
                minSectionPosition,
                maxSectionPosition,
                biomesCopy,
                sectionCount,
                lightCopy,
                skyLightCopy,
                tiles != null ? new BlockVector3ChunkMap<>(tiles) : null,
                entities != null ? new HashSet<>(entities) : null,
                entityRemoves != null ? new HashSet<>(entityRemoves) : null,
                heightMaps != null ? new HashMap<>(heightMaps) : null,
                defaultOrdinal,
                fastMode,
                bitMask
        );
    }

    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }

    // Checks and updates the various section arrays against the new layer index
    private void updateSectionIndexRange(int layer) {
        if (layer >= minSectionPosition && layer <= maxSectionPosition) {
            return;
        }
        if (layer < minSectionPosition) {
            int diff = minSectionPosition - layer;
            sectionCount += diff;
            minSectionPosition = layer;
            resizeSectionsArrays(layer, diff, false); // prepend new layer(s)
        } else {
            int diff = layer - maxSectionPosition;
            sectionCount += diff;
            maxSectionPosition = layer;
            resizeSectionsArrays(layer, diff, true); // append new layer(s)
        }
    }

    private void resizeSectionsArrays(int layer, int diff, boolean appendNew) {
        char[][] tmpBlocks = new char[sectionCount][];
        int destPos = appendNew ? 0 : diff;
        System.arraycopy(blocks, 0, tmpBlocks, destPos, blocks.length);
        blocks = tmpBlocks;
        if (biomes != null) {
            BiomeType[][] tmpBiomes = new BiomeType[sectionCount][64];
            System.arraycopy(biomes, 0, tmpBiomes, destPos, biomes.length);
            biomes = tmpBiomes;
        }
        if (light != null) {
            char[][] tmplight = new char[sectionCount][];
            System.arraycopy(light, 0, tmplight, destPos, light.length);
            light = tmplight;
        }
        if (skyLight != null) {
            char[][] tmplight = new char[sectionCount][];
            System.arraycopy(skyLight, 0, tmplight, destPos, skyLight.length);
            skyLight = tmplight;
        }
    }

}
