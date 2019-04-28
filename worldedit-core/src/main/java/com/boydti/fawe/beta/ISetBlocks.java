package com.boydti.fawe.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

/**
 * Interface for setting blocks
 */
public interface ISetBlocks extends IBlocks {
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder holder);

    boolean isEmpty();
}
