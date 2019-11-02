package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public interface DelegateChunkSet extends IChunkSet {

    IChunkSet getParent();

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder holder) {
        return getParent().setBlock(x, y, z, holder);
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tile) {
        return getParent().setTile(x, y, z, tile);
    }

    @Override
    default void setEntity(CompoundTag tag) {
        getParent().setEntity(tag);
    }

    @Override
    default void removeEntity(UUID uuid) {
        getParent().removeEntity(uuid);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        return getParent().getBlock(x, y, z);
    }

    @Override
    default char[] getArray(int layer) {
        return getParent().getArray(layer);
    }

    @Override
    default BiomeType[] getBiomes() {
        return getParent().getBiomes();
    }

    @Override
    default Map<BlockVector3, CompoundTag> getTiles() {
        return getParent().getTiles();
    }

    @Override
    default Set<CompoundTag> getEntities() {
        return getParent().getEntities();
    }

    @Override
    default Set<UUID> getEntityRemoves() {
        return getParent().getEntityRemoves();
    }

    @Override
    default IChunkSet reset() {
        IChunkSet parent = getParent();
        parent.reset();
        return parent;
    }

    @Override
    @Nullable
    default Operation commit() {
        return getParent().commit();
    }

    @Override
    default boolean hasSection(int layer) {
        return getParent().hasSection(layer);
    }

    @Override
    default boolean trim(boolean aggressive) {
        return getParent().trim(aggressive);
    }

    @Override
    default <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
        throws WorldEditException {
        return getParent().setBlock(position, block);
    }

    @Override
    default boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getParent().setBiome(position, biome);
    }
}
