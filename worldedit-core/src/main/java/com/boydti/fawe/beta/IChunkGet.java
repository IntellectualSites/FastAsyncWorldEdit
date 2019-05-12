package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.InputExtent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Interface for getting blocks
 */
public interface IChunkGet extends IBlocks, Trimable, InputExtent {
    @Override
    BaseBlock getFullBlock(int x, int y, int z);

    @Override
    BiomeType getBiome(int x, int z);

    @Override
    BlockState getBlock(int x, int y, int z);

    CompoundTag getTag(int x, int y, int z);

    @Override
    boolean trim(boolean aggressive);

    default void optimize() {

    }
}
