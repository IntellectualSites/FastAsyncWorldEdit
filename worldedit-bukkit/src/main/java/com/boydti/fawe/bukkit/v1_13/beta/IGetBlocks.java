package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public interface IGetBlocks extends IBlocks {
    BaseBlock getFullBlock(int x, int y, int z);

    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);
}
