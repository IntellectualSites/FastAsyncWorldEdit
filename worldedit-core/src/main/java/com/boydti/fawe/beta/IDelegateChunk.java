package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * Delegate for IChunk
 *
 * @param <U> parent class
 */
public interface IDelegateChunk<U extends IChunk> extends IChunk {

    U getParent();

    @Override
    default IChunk getRoot() {
        IChunk root = getParent();
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
        }
        return root;
    }


    @Override
    default IQueueExtent getQueue() {
        return getParent().getQueue();
    }

    @Override
    default CompoundTag getTag(int x, int y, int z) {
        return getParent().getTag(x, y, z);
    }

    @Override
    default boolean hasSection(int layer) {
        return getParent().hasSection(layer);
    }

    @Override
    default void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
        getParent().flood(flood, mask, block);
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
    default void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region) {
        getParent().filterBlocks(filter, block, region);
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
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
