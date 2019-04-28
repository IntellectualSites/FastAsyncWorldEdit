package com.boydti.fawe.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IChunk<T, V extends IQueueExtent> {
    /* set */
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder block);

    /* get */
    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    BaseBlock getFullBlock(int x, int y, int z);

    void init(V extent, int X, int Z);

    T apply();

    int getX();

    int getZ();

    default IChunk getRoot() {
        return this;
    }

    void filter(Filter filter);
}
