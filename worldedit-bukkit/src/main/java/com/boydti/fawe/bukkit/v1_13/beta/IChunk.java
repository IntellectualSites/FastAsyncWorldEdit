package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IChunk {
    /* set */
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder holder);

    /* get */
    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    BaseBlock getFullBlock(int x, int y, int z);

    void init(SingleThreadQueueExtent extent, int X, int Z);

    int getX();

    int getZ();

    void apply();

    default IChunk getRoot() {
        return this;
    }

    void filter(Filter filter);
}
