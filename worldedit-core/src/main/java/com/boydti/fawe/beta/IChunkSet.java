package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Interface for setting blocks
 */
public interface IChunkSet extends IBlocks, OutputExtent {

    @Override
    boolean setBiome(int x, int y, int z, BiomeType biome);

    @Override
    boolean setBlock(int x, int y, int z, BlockStateHolder holder);

    boolean isEmpty();

    @Override
    boolean setTile(int x, int y, int z, CompoundTag tile);

    void setEntity(CompoundTag tag);

    void removeEntity(UUID uuid);

    BlockState getBlock(int x, int y, int z);

    char[] getArray(int layer);

    BiomeType[] getBiomes();

    Map<Short, CompoundTag> getTiles();

    Set<CompoundTag> getEntities();

    Set<UUID> getEntityRemoves();

    @Override
    IChunkSet reset();

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }
}
