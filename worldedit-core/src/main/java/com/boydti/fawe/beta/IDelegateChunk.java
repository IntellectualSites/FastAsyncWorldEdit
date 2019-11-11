package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * Delegate for IChunk
 *
 * @param <U> parent class
 */
public interface IDelegateChunk<U extends IQueueChunk> extends IQueueChunk {

    U getParent();

    @Override
    default IQueueChunk getRoot() {
        IQueueChunk root = getParent();
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
        }
        return root;
    }

    @Override
    default <T extends Future<T>> T call(IChunkSet set, Runnable finalize) {
        return getParent().call(set, finalize);
    }

    @Override
    default CompoundTag getTag(int x, int y, int z) {
        return getParent().getTag(x, y, z);
    }

    @Override
    default boolean hasSection(int layer) {
        return getParent().hasSection(layer);
    }

//    @Override
//    default void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
//        getParent().flood(flood, mask, block);
//    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tag) {
        return getParent().setTile(x, y, z, tag);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder holder) {
        return getParent().setBlock(x, y, z, holder);
    }

    @Override
    default BiomeType getBiomeType(int x, int z) {
        return getParent().getBiomeType(x, z);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        return getParent().getBlock(x, y, z);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    default void init(IQueueExtent extent, int chunkX, int chunkZ) {
        getParent().init(extent, chunkX, chunkZ);
    }

    @Override
    default int getX() {
        return getParent().getX();
    }

    @Override
    default int getZ() {
        return getParent().getZ();
    }


    @Override
    default boolean trim(boolean aggressive) {
        return getParent().trim(aggressive);
    }

    @Override
    default Future call() {
        return getParent().call();
    }

    @Override
    default void join() throws ExecutionException, InterruptedException {
        getParent().join();
    }

    @Override
    default void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, boolean full) {
        getParent().filterBlocks(filter, block, region, full);
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
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
    default CompoundTag getEntity(UUID uuid) {
        return getParent().getEntity(uuid);
    }

    @Override
    default char[] load(int layer) {
        return getParent().load(layer);
    }

    @Override
    default void setBlocks(int layer, char[] data) {
        getParent().setBlocks(layer, data);
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
    default Set<UUID> getEntityRemoves() {
        return getParent().getEntityRemoves();
    }

    @Override
    default BiomeType[] getBiomes() {
        return getParent().getBiomes();
    }

    default <T extends IChunk> T findParent(Class<T> clazz) {
        IChunk root = getParent();
        if (clazz.isAssignableFrom(root.getClass())) {
            return (T) root;
        }
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
            if (clazz.isAssignableFrom(root.getClass())) {
                return (T) root;
            }
        }
        return null;
    }
}
