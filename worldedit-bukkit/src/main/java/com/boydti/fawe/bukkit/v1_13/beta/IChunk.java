package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IChunk {
    /* set */
    void setBiome(int x, int z, BiomeType biome);

    void setBlock(int x, int y, int z, BlockStateHolder holder);

    /* get */
    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    BaseBlock getFullBlock(int x, int y, int z);
}
