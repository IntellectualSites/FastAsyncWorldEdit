package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Interface for getting blocks
 */
public interface IGetBlocks extends IBlocks, Trimable {
    BaseBlock getFullBlock(int x, int y, int z);

    BiomeType getBiome(int x, int z);

    BlockState getBlock(int x, int y, int z);

    CompoundTag getTag(int x, int y, int z);

    @Override
    boolean trim(boolean aggressive);

    void filter(Filter filter, FilterBlock block);

    default void optimize() {

    }
}
