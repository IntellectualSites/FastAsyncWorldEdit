package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.Pool;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class CharSetBlocks extends CharBlocks implements IChunkSet {

    private static final Pool<CharSetBlocks> POOL = FaweCache.IMP.registerPool(
            CharSetBlocks.class,
            CharSetBlocks::new,
            Settings.IMP.QUEUE.POOL
    );

    public static CharSetBlocks newInstance() {
        return POOL.poll();
    }

    public BiomeType[] biomes;
    public char[][] light;
    public char[][] skyLight;
    public BlockVector3ChunkMap<CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;
    public Map<HeightMapType, int[]> heightMaps;
    private boolean fastMode = false;
    private int bitMask = -1;

    private CharSetBlocks() {
        // Expand as we go
        super(0, 15);
    }

    @Override
    public synchronized void recycle() {
        POOL.offer(this);
    }

    @Override
    public BiomeType[] getBiomes() {
        return biomes;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        if (biomes == null || (y >> 4) < minLayer || (y >> 4) > maxLayer) {
            return null;
        }
        y -= minLayer << 4;
        return biomes[(y >> 2) << 4 | (z >> 2) << 2 | x >> 2];
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
    public Set<UUID> getEntityRemoves() {
        return entityRemoves == null ? Collections.emptySet() : entityRemoves;
    }

    @Override
    public Map<HeightMapType, int[]> getHeightMaps() {
        return heightMaps == null ? new HashMap<>() : heightMaps;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        checkLayer(y >> 4);
        y -= minLayer << 4;
        if (biomes == null) {
            biomes = new BiomeType[1024];
        }
        biomes[(y >> 2) << 4 | (z >> 2) << 2 | x >> 2] = biome;
        return true;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder) {
        checkLayer(y >> 4);
        set(x, y, z, holder.getOrdinalChar());
        holder.applyTileEntity(this, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        checkLayer(layer);
        layer -= minLayer;
        this.blocks[layer] = data;
        this.sections[layer] = data == null ? empty : FULL;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
            throws WorldEditException {
        return setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        if (tiles == null) {
            tiles = new BlockVector3ChunkMap<>();
        }
        checkLayer(y >> 4);
        tiles.put(x, y, z, tile);
        return true;
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
        checkLayer(y >> 4);
        if (light == null) {
            light = new char[layers][];
        }
        final int layer = (y >> 4) - minLayer;
        if (light[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            light[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        light[y >> 4][index] = (char) value;
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
        checkLayer(y >> 4);
        if (skyLight == null) {
            skyLight = new char[layers][];
        }
        final int layer = (y >> 4) - minLayer;
        if (skyLight[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            skyLight[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        skyLight[y >> 4][index] = (char) value;
    }

    @Override
    public void setHeightMap(HeightMapType type, int[] heightMap) {
        if (heightMaps == null) {
            heightMaps = new HashMap<>();
        }
        heightMaps.put(type, heightMap);
    }

    @Override
    public void setLightLayer(int layer, char[] toSet) {
        checkLayer(layer);
        if (light == null) {
            light = new char[layers][];
        }
        layer -= minLayer;
        light[layer] = toSet;
    }

    @Override
    public void setSkyLightLayer(int layer, char[] toSet) {
        checkLayer(layer);
        if (skyLight == null) {
            skyLight = new char[layers][];
        }
        layer -= minLayer;
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
        checkLayer(layer);
        layer -= minLayer;
        if (light == null) {
            light = new char[layers][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 0);
        if (sky) {
            if (skyLight == null) {
                skyLight = new char[layers][];
            }
            if (skyLight[layer] == null) {
                skyLight[layer] = new char[4096];
            }
            Arrays.fill(skyLight[layer], (char) 0);
        }
    }

    @Override
    public void setFullBright(int layer) {
        checkLayer(layer);
        layer -= minLayer;
        if (light == null) {
            light = new char[layers][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        if (skyLight == null) {
            skyLight = new char[layers][];
        }
        if (skyLight[layer] == null) {
            skyLight[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 15);
        Arrays.fill(skyLight[layer], (char) 15);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
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
    public boolean isEmpty() {
        if (biomes != null || light != null || skyLight != null) {
            return false;
        }
        return IntStream.range(minLayer, maxLayer).noneMatch(this::hasSection);
    }

    @Override
    public IChunkSet reset() {
        biomes = null;
        tiles = null;
        entities = null;
        entityRemoves = null;
        super.reset();
        return null;
    }

    @Override
    public char[] load(final int layer) {
        checkLayer(layer);
        return super.load(layer);
    }

    @Override
    public int getLayerCount() {
        return layers;
    }

    private void checkLayer(int layer) {
        if (layer >= minLayer && layer <= maxLayer) {
            return;
        }
        if (layer < minLayer) {
            int diff = minLayer - layer;
            layers += diff;
            char[][] tmpBlocks = new char[layers][];
            Section[] tmpSections = new Section[layers];
            System.arraycopy(blocks, 0, tmpBlocks, diff, blocks.length);
            System.arraycopy(sections, 0, tmpSections, diff, sections.length);
            for (int i = 0; i < diff; i++) {
                tmpSections[i] = empty;
            }
            blocks = tmpBlocks;
            sections = tmpSections;
            minLayer = layer;
        } else {
            int diff = layer - maxLayer;
            layers += diff;
            char[][] tmpBlocks = new char[layers][];
            Section[] tmpSections = new Section[layers];
            System.arraycopy(blocks, 0, tmpBlocks, 0, blocks.length);
            System.arraycopy(sections, 0, tmpSections, 0, sections.length);
            for (int i = layers - diff; i < layers; i++) {
                tmpSections[i] = empty;
            }
            blocks = tmpBlocks;
            sections = tmpSections;
            maxLayer = layer;
        }
    }

}
