package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IChunk;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IChunkExtent<T extends IChunk> extends Extent {
    /**
     * Get the IChunk at a position (and cache it if it's not already)
     *
     * @param x
     * @param z
     * @return IChunk
     */
    T getOrCreateChunk(int chunkX, int chunkZ);

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder state) {
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

    default BiomeType getBiome(int x, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBiomeType(x & 15, z & 15);
    }
}
