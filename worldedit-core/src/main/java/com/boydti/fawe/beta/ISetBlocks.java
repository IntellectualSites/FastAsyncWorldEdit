package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashSet;
import java.util.UUID;

/**
 * Interface for setting blocks
 */
public interface ISetBlocks extends IBlocks {
    boolean setBiome(int x, int y, int z, BiomeType biome);

    boolean setBlock(int x, int y, int z, BlockStateHolder holder);

    boolean isEmpty();

    void setTile(int x, int y, int z, CompoundTag tile);

    void setEntity(CompoundTag tag);

    void removeEntity(UUID uuid);

    default void optimize() {

    }
}
