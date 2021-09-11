package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.collection.MemBlockSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BitSetBlocks implements IChunkSet {

    private final MemBlockSet.RowZ row;
    private final BlockState blockState;
    private final int minSectionIndex;
    private final int maxSectionIndex;
    private final int layers;

    public BitSetBlocks(BlockState blockState, int minSectionIndex, int maxSectionIndex) {
        this.row = new MemBlockSet.RowZ(minSectionIndex, maxSectionIndex);
        this.blockState = blockState;
        this.minSectionIndex = minSectionIndex;
        this.maxSectionIndex = maxSectionIndex;
        this.layers = maxSectionIndex - minSectionIndex + 1;
    }

    @Override
    public boolean hasSection(int layer) {
        layer -= minSectionIndex;
        return row.rows[layer] != MemBlockSet.NULL_ROW_Y;
    }


    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder) {
        y -= minSectionIndex << 4;
        row.set(null, x, y, z, minSectionIndex, maxSectionIndex);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        layer -= minSectionIndex;
        row.reset(layer);
        int by = layer << 4;
        for (int y = 0, index = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, index++) {
                    if (data[index] != 0) {
                        row.set(null, x, by + y, z, minSectionIndex, maxSectionIndex);
                    }
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return row.isEmpty();
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        return false;
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
    }

    @Override
    public void setHeightMap(HeightMapType type, int[] heightMap) {
    }

    @Override
    public void setLightLayer(int layer, char[] toSet) {
    }

    @Override
    public void setSkyLightLayer(int layer, char[] toSet) {
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
    }

    @Override
    public void setFullBright(int layer) {
    }

    @Override
    public void setEntity(CompoundTag tag) {
    }

    @Override
    public void removeEntity(UUID uuid) {
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (row.get(null, x, y, z)) {
            return blockState;
        }
        return null;
    }

    @Override
    public char[] load(int layer) {
        layer -= minSectionIndex;
        char[] arr = FaweCache.IMP.SECTION_BITS_TO_CHAR.get();
        MemBlockSet.IRow nullRowY = row.getRow(layer);
        if (nullRowY instanceof MemBlockSet.RowY) {
            char value = blockState.getOrdinalChar();
            MemBlockSet.RowY rowY = (MemBlockSet.RowY) nullRowY;
            long[] bits = rowY.getBits();
            for (int y = 0, longIndex = 0, blockIndex = 0; y < 16; y++) {
                for (int z = 0; z < 16; z += 4, longIndex++, blockIndex += 64) {
                    long bitBuffer = bits[longIndex];
                    if (bitBuffer != 0) {
                        if (bitBuffer == -1L) {
                            Arrays.fill(arr, blockIndex, blockIndex + 64, value);
                            continue;
                        }
                        Arrays.fill(arr, Character.MIN_VALUE);
                        do {
                            final long lowBit = Long.lowestOneBit(bitBuffer);
                            final int bitIndex = Long.bitCount(lowBit - 1);
                            arr[blockIndex + bitIndex] = value;
                            bitBuffer = bitBuffer ^ lowBit;
                        } while (bitBuffer != 0);
                    }

                }
            }
        }
        return arr;
    }

    @Override
    public BiomeType[] getBiomes() {
        return null;
    }

    @Override
    public char[][] getLight() {
        return new char[0][];
    }

    @Override
    public char[][] getSkyLight() {
        return new char[0][];
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return null;
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return Collections.emptyMap();
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return Collections.emptySet();
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return null;
    }

    @Override
    public IChunkSet reset() {
        row.reset();
        return this;
    }

    @Override
    public boolean hasBiomes(final int layer) {
        return false;
    }

    @Override
    public int getSectionCount() {
        return layers;
    }

    @Override
    public int getMaxSectionIndex() {
        return minSectionIndex;
    }

    @Override
    public int getMinSectionIndex() {
        return maxSectionIndex;
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
