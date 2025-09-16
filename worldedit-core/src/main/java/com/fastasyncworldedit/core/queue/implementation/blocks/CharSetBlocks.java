package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.Pool;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CharSetBlocks extends CharBlocks implements IChunkSet {

    private static final Pool<CharSetBlocks> POOL = FaweCache.INSTANCE.registerPool(
            CharSetBlocks.class,
            CharSetBlocks::new, Settings.settings().QUEUE.POOL
    );

    /**
     * @deprecated Use {@link CharSetBlocks#newInstance(int, int)}
     */
    @Deprecated(forRemoval = true, since = "2.13.0")
    public static CharSetBlocks newInstance() {
        return POOL.poll();
    }

    /**
     * Create a new {@link CharSetBlocks} instance
     *
     * @param x chunk x
     * @param z chunk z
     * @return New pooled CharSetBlocks instance.
     */
    public static CharSetBlocks newInstance(int x, int z) {
        CharSetBlocks set = POOL.poll();
        set.init(x, z);
        return set;
    }

    public BiomeType[][] biomes;
    public char[][] light;
    public char[][] skyLight;
    public BlockVector3ChunkMap<FaweCompoundTag> tiles;
    public HashSet<FaweCompoundTag> entities;
    public HashSet<UUID> entityRemoves;
    public EnumMap<HeightMapType, int[]> heightMaps;
    private boolean fastMode = false;
    private int bitMask = -1;
    private SideEffectSet sideEffectSet = SideEffectSet.defaults();

    private CharSetBlocks() {
        // Expand as we go
        super(0, 15);
    }

    @Override
    public void recycle() {
        reset();
        POOL.offer(this);
    }

    @Override
    public BiomeType[][] getBiomes() {
        return biomes;
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
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return tiles == null ? Collections.emptyMap() : tiles;
    }

    @Override
    public @Nullable FaweCompoundTag tile(final int x, final int y, final int z) {
        return tiles == null ? null : tiles.get(x, y, z);
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return entities == null ? Collections.emptySet() : entities;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return entityRemoves == null ? Collections.emptySet() : entityRemoves;
    }

    @Override
    public Map<HeightMapType, int[]> getHeightMaps() {
        return heightMaps == null ? new EnumMap<>(HeightMapType.class) : heightMaps;
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
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
            throws WorldEditException {
        return setBlock(position.x(), position.y(), position.z(), block);
    }

    @Override
    public boolean tile(final int x, final int y, final int z, final FaweCompoundTag tag) {
        if (tiles == null) {
            tiles = new BlockVector3ChunkMap<>();
        }
        updateSectionIndexRange(y >> 4);
        tiles.put(x, y, z, tag);
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
    public char[][] getLight() {
        return light;
    }

    @Override
    public char[][] getSkyLight() {
        return skyLight;
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
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

    @Override
    public void entity(final FaweCompoundTag tag) {
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
    public IChunkSet reset() {
        biomes = null;
        tiles = null;
        entities = null;
        entityRemoves = null;
        light = null;
        skyLight = null;
        heightMaps = null;
        super.reset();
        return null;
    }

    @Override
    public boolean hasBiomes(int layer) {
        layer -= minSectionPosition;
        if (layer < 0 || layer >= blocks.length) {
            return false;
        }
        return biomes != null && biomes[layer] != null;
    }

    @Override
    public ThreadUnsafeCharBlocks createCopy() {
        char[][] blocksCopy = new char[sectionCount][];
        for (int i = 0; i < sectionCount; i++) {
            if (blocks[i] != null) {
                blocksCopy[i] = new char[FaweCache.INSTANCE.BLOCKS_PER_LAYER];
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
        char[][] lightCopy = createLightCopy(light, sectionCount);
        char[][] skyLightCopy = createLightCopy(skyLight, sectionCount);
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
                heightMaps != null ? new EnumMap<>(heightMaps) : null,
                defaultOrdinal(),
                fastMode,
                bitMask,
                sideEffectSet,
                getX(),
                getZ()
        );
    }

    @Override
    public void setSideEffectSet(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    @Override
    public SideEffectSet getSideEffectSet() {
        return sideEffectSet;
    }

    static char[][] createLightCopy(char[][] lightArr, int sectionCount) {
        if (lightArr == null) {
            return null;
        } else {
            char[][] lightCopy = new char[sectionCount][];
            for (int i = 0; i < sectionCount; i++) {
                if (lightArr[i] != null) {
                    lightCopy[i] = new char[lightArr[i].length];
                    System.arraycopy(lightArr[i], 0, lightCopy[i], 0, lightArr[i].length);
                }
            }
            return lightCopy;
        }
    }

    @Override
    public char[] load(final int layer) {
        updateSectionIndexRange(layer);
        return super.load(layer);
    }

    @Override
    protected char defaultOrdinal() {
        return BlockTypesCache.ReservedIDs.__RESERVED__;
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
            resizeSectionsArrays(diff, false); // prepend new layer(s)
        } else {
            int diff = layer - maxSectionPosition;
            sectionCount += diff;
            maxSectionPosition = layer;
            resizeSectionsArrays(diff, true); // append new layer(s)
        }
    }

    private void resizeSectionsArrays(int diff, boolean appendNew) {
        char[][] tmpBlocks = new char[sectionCount][];
        Object[] tmpSectionLocks = new Object[sectionCount];
        int destPos = appendNew ? 0 : diff;
        System.arraycopy(blocks, 0, tmpBlocks, destPos, blocks.length);
        System.arraycopy(sectionLocks, 0, tmpSectionLocks, destPos, blocks.length);
        int toFillFrom = appendNew ? sectionCount - diff : 0;
        int toFillTo = appendNew ? sectionCount : diff;
        for (int i = toFillFrom; i < toFillTo; i++) {
            tmpSectionLocks[i] = new Object();
        }
        blocks = tmpBlocks;
        sectionLocks = tmpSectionLocks;
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
