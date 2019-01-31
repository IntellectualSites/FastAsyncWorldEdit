package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.*;

public abstract class IntFaweChunk<T, V extends FaweQueue> extends FaweChunk<T> {

    public final int[][] ids;
    public final short[] count;
    public final short[] air;
    public final byte[] heightMap;

    public byte[] biomes;
    public HashMap<Short, CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;

    public T chunk;

    public IntFaweChunk(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z);
        this.ids = ids;
        this.count = count;
        this.air = air;
        this.heightMap = heightMap;
    }

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public IntFaweChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.ids = new int[HEIGHT >> 4][];
        this.count = new short[HEIGHT >> 4];
        this.air = new short[HEIGHT >> 4];
        this.heightMap = new byte[256];
    }

    @Override
    public V getParent() {
        return (V) super.getParent();
    }

    @Override
    public T getChunk() {
        if (this.chunk == null) {
            this.chunk = getNewChunk();
        }
        return this.chunk;
    }

    public abstract T getNewChunk();

    @Override
    public void setLoc(final FaweQueue parent, int x, int z) {
        super.setLoc(parent, x, z);
        this.chunk = null;
    }

    /**
     * Get the number of block changes in a specified section
     *
     * @param i
     * @return
     */
    public int getCount(final int i) {
        return this.count[i];
    }

    public int getAir(final int i) {
        return this.air[i];
    }

    public void setCount(final int i, final short value) {
        this.count[i] = value;
    }

    public int getTotalCount() {
        int total = 0;
        for (int i = 0; i < count.length; i++) {
            total += Math.min(4096, this.count[i]);
        }
        return total;
    }

    public int getTotalAir() {
        int total = 0;
        for (int i = 0; i < air.length; i++) {
            total += Math.min(4096, this.air[i]);
        }
        return total;
    }

    @Override
    public int getBitMask() {
        int bitMask = 0;
        for (int section = 0; section < ids.length; section++) {
            if (ids[section] != null) {
                bitMask += 1 << section;
            }
        }
        return bitMask;
    }

    /**
     * Get the raw data for a section
     *
     * @param i
     * @return
     */
    @Override
    public int[] getIdArray(final int i) {
        return this.ids[i];
    }

    @Override
    public int[][] getCombinedIdArrays() {
        return this.ids;
    }

    @Override
    public byte[] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        short i = FaweCache.CACHE_I[y][z][x];
        int[] array = getIdArray(i);
        if (array == null) {
            return 0;
        }
        return array[FaweCache.CACHE_J[y][z][x]];
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        if (tiles == null) {
            tiles = new HashMap<>();
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        tiles.put(pair, tile);
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<Short, CompoundTag>() : tiles;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities == null ? Collections.emptySet() : entities;
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
    public HashSet<UUID> getEntityRemoves() {
        return entityRemoves == null ? new HashSet<UUID>() : entityRemoves;
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        final int i = FaweCache.CACHE_I[y][z][x];
        final int j = FaweCache.CACHE_J[y][z][x];
        int[] vs = this.ids[i];
        if (vs == null) {
            vs = this.ids[i] = new int[4096];
        }
        vs[j] = combinedId;
        this.count[i]++;
        switch (BlockTypes.getFromStateId(combinedId).getResource().toUpperCase()) {
            case "AIR":
            case "CAVE_AIR":
            case "VOID_AIR":
                this.air[i]++;
                return;
            default:
                heightMap[z << 4 | x] = (byte) y;
                return;
        }
    }

    @Deprecated
    public void setBitMask(int ignore) {
        // Remove
    }

    @Override
    public void setBiome(final int x, final int z, byte biome) {
        if (this.biomes == null) {
            this.biomes = new byte[256];
        }
        if (biome == 0) biome = -1;
        biomes[((z & 15) << 4) + (x & 15)] = biome;
    }

    @Override
    public abstract IntFaweChunk<T, V> copy(boolean shallow);
}
