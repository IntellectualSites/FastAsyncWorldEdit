package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.queue.Pool;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.BlockVector3ChunkMap;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class CharSetBlocks extends CharBlocks implements IChunkSet {
    private static final Pool<CharSetBlocks> POOL = FaweCache.IMP.registerPool(CharSetBlocks.class, CharSetBlocks::new, Settings.IMP.QUEUE.POOL);
    public static CharSetBlocks newInstance() {
        return POOL.poll();
    }

    public BiomeType[] biomes;
    public char[][] light;
    public char[][] skyLight;
    public BlockVector3ChunkMap<CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;
    private boolean fastMode = false;
    private int bitMask = -1;

    private CharSetBlocks() {}

    @Override
    public void recycle() {
        POOL.offer(this);
    }

    @Override
    public BiomeType[] getBiomes() {
        return biomes;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        if (biomes == null) {
            return null;
        }
        return biomes[(z << 4) | x];
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
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[256];
        }
        biomes[x + (z << 4)] = biome;
        return true;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, @Range(from = 0, to = 255) int y, int z, T holder) {
        set(x, y, z, holder.getOrdinalChar());
        holder.applyTileEntity(this, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        this.blocks[layer] = data;
        this.sections[layer] = data == null ? EMPTY : FULL;
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
        tiles.put(x, y, z, tile);
        return true;
    }

    @Override public void setBlockLight(int x, int y, int z, int value) {
        if (light == null) {
            light = new char[16][];
        }
        final int layer = y >> 4;
        if (light[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            light[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        light[y >> 4][index] = (char) value;
    }

    @Override public void setSkyLight(int x, int y, int z, int value) {
        if (skyLight == null) {
            skyLight = new char[16][];
        }
        final int layer = y >> 4;
        if (skyLight[layer] == null) {
            char[] c = new char[4096];
            Arrays.fill(c, (char) 16);
            skyLight[layer] = c;
        }
        final int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        skyLight[y >> 4][index] = (char) value;
    }

    @Override public void setLightLayer(int layer, char[] toSet) {
        if (light == null) {
            light = new char[16][];
        }
        light[layer] = toSet;
    }

    @Override public void setSkyLightLayer(int layer, char[] toSet) {
        if (skyLight == null) {
            skyLight = new char[16][];
        }
        skyLight[layer] = toSet;
    }

    @Override public char[][] getLight() {
        return light;
    }

    @Override public char[][] getSkyLight() {
        return skyLight;
    }

    @Override public void removeSectionLighting(int layer, boolean sky) {
        if (light == null) {
            light = new char[16][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 0);
        if (sky) {
            if (skyLight == null) {
                skyLight = new char[16][];
            }
            if (skyLight[layer] == null) {
                skyLight[layer] = new char[4096];
            }
            Arrays.fill(skyLight[layer], (char) 0);
        }
    }

    @Override public void setFullBright(int layer) {
        if (light == null) {
            light = new char[16][];
        }
        if (light[layer] == null) {
            light[layer] = new char[4096];
        }
        if (skyLight == null) {
            skyLight = new char[16][];
        }
        if (skyLight[layer] == null) {
            skyLight[layer] = new char[4096];
        }
        Arrays.fill(light[layer], (char) 15);
        Arrays.fill(skyLight[layer], (char) 15);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return setBiome(position.getX(),0, position.getZ(), biome);
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
        return IntStream.range(0, 16).noneMatch(this::hasSection);
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
}
