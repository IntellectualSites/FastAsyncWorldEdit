package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;
import com.boydti.fawe.bukkit.v1_13.beta.holder.ChunkHolder;
import com.boydti.fawe.bukkit.v1_13.beta.holder.IDelegateChunk;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IQueueExtent {
    void init(World world);

    IChunk getCachedChunk(int X, int Z);

    default boolean setBlock(int x, int y, int z, BlockStateHolder state) {
        IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
    }

    default BlockState getBlock(int x, int y, int z) {
        IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    default BiomeType getBiome(int x, int z) {
        IChunk chunk = getCachedChunk(x >> 4, z >> 4);
        return chunk.getBiome(x & 15, z & 15);
    }

    // Return ChunkHolder
    ChunkHolder create(boolean full);

    // Region restrictions
    IDelegateChunk wrap(IChunk root);
}