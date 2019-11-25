package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.InputExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * An interface for getting blocks.
 */
public interface IChunkGet extends IBlocks, Trimable, InputExtent {

    @Override
    BaseBlock getFullBlock(int x, int y, int z);

    @Override
    BiomeType getBiomeType(int x, int z);

    @Override
    BlockState getBlock(int x, int y, int z);

    CompoundTag getTag(int x, int y, int z);

    @Override
    Map<BlockVector3, CompoundTag> getTiles();

    @Override
    Set<CompoundTag> getEntities();

    @Override
    boolean trim(boolean aggressive);

    default void optimize() {

    }

    <T extends Future<T>> T call(IChunkSet set, Runnable finalize);

    @Override
    char[] load(int layer);

    CompoundTag getEntity(UUID uuid);
}
