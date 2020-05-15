package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IChunk;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.Range;

public interface IChunkExtent<T extends IChunk> extends Extent {
    /**
     * Get the IChunk at a position (and cache it if it's not already)
     *
     * @param chunkX
     * @param chunkZ
     * @return IChunk
     */
    T getOrCreateChunk(int chunkX, int chunkZ);

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, @Range(from = 0, to = 255) int y, int z, B state) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setTile(x & 15, y, z & 15, tile);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getFullBlock(x & 15, y, z & 15);
    }

    @Override
    default BiomeType getBiomeType(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBiomeType(x & 15, y, z & 15);
    }

    @Override
    default void setSkyLight(int x, int y, int z, int value) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.setSkyLight(x, y, z, value);
    }

    @Override
    default void setBlockLight(int x, int y, int z, int value) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.setSkyLight(x, y, z, value);
    }

    @Override
    default int getLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getLight(x, y, z);
    }

    @Override
    default int getSkyLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getSkyLight(x, y, z);
    }

    @Override
    default int getEmmittedLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getEmmittedLight(x, y, z);
    }

    @Override
    default int getBrightness(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBrightness(x, y, z);
    }

    @Override
    default int getOpacity(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getOpacity(x, y, z);
    }
}
