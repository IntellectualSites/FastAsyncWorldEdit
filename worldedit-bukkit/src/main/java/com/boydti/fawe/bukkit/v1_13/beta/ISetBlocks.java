package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface ISetBlocks extends IBlocks {
    void setBiome(int x, int z, BiomeType biome);

    void setBlock(int x, int y, int z, BlockStateHolder holder);
}
