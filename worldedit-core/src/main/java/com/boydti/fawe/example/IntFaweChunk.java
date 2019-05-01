package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;

import java.util.*;

public abstract class IntFaweChunk<T, V extends FaweQueue> extends FaweChunk<T> {

    public final int[][] setBlocks;
    public final short[] count;
    public final short[] air;

    public BiomeType[] biomes;
    public HashMap<Short, CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;

    public T chunk;

    public IntFaweChunk(FaweQueue parent, int x, int z, int[][] setBlocks, short[] count, short[] air) {
        super(parent, x, z);
        this.setBlocks = setBlocks;
        this.count = count;
        this.air = air;
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
        this.setBlocks = new int[HEIGHT >> 4][];
        this.count = new short[HEIGHT >> 4];
        this.air = new short[HEIGHT >> 4];
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
        for (int section = 0; section < setBlocks.length; section++) {
            if (setBlocks[section] != null) {
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
        return this.setBlocks[i];
    }

    @Override
    public int[][] getCombinedIdArrays() {
        return this.setBlocks;
    }

    @Override
    public BiomeType[] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        int[] array = getIdArray(y >> 4);
        if (array == null) {
            return 0;
        }
        return array[(((y & 0xF) << 8) | (z << 4) | x)];
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
        return tiles == null ? new HashMap<>() : tiles;
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
        return entityRemoves == null ? new HashSet<>() : entityRemoves;
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        final int i = y >> 4;
        int[] vs = this.setBlocks[i];
        if (vs == null) {
            vs = this.setBlocks[i] = new int[4096];
        }
        int index = (((y & 15) << 8) | (z << 4) | x);
        int existing = vs[index];
        vs[index] = combinedId;
        switch (existing) {
            case 0:
                this.count[i]++;
                switch (combinedId) {
                    case 0:
                    case BlockID.AIR:
                    case BlockID.CAVE_AIR:
                    case BlockID.VOID_AIR:
                        this.air[i]++;
                }
                break;
            case BlockID.AIR:
            case BlockID.CAVE_AIR:
            case BlockID.VOID_AIR:
                switch (combinedId) {
                    case 0:
                    case BlockID.AIR:
                    case BlockID.CAVE_AIR:
                    case BlockID.VOID_AIR:
                        break;
                    default:
                        this.air[i]--;

                }
        }
        return;
    }

    @Deprecated
    public void setBitMask(int ignore) {
        // Remove
    }

    @Override
    public void setBiome(final int x, final int z, BiomeType biome) {
        if (this.biomes == null) {
            this.biomes = new BiomeType[256];
        }
        biomes[((z & 15) << 4) + (x & 15)] = biome;
    }

    @Override
    public abstract IntFaweChunk<T, V> copy(boolean shallow);
}
