package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.collection.MemBlockSet;
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

    public BitSetBlocks(BlockState blockState) {
        this.row = new MemBlockSet.RowZ();
        this.blockState = blockState;
    }

    @Override
    public boolean hasSection(int layer) {
        return row.rows[layer] != MemBlockSet.NULL_ROW_Y;
    }


    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder) {
        row.set(null, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        row.reset(layer);
        int by = layer << 4;
        for (int y = 0, index = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, index++) {
                    if (data[index] != 0) {
                        row.set(null, x, by + y, z);
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
        char[] arr = FaweCache.INSTANCE.getSectionBitsToChar().get();
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
    public boolean trim(boolean aggressive) {
        return false;
    }
}
