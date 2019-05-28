package com.boydti.fawe.beta;

import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Delegate for IChunk
 * @param <U> parent class
 */
public interface IDelegateChunk<U extends IChunk> extends IChunk {
    U getParent();

    default IChunk getRoot() {
        IChunk root = getParent();
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
        }
        return root;
    }

    @Override
    default void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
        getParent().flood(flood, mask, block);
    }

    @Override
    default boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    default boolean setBlock(final int x, final int y, final int z, final BlockStateHolder holder) {
        return getParent().setBlock(x, y, z, holder);
    }

    @Override
    default BiomeType getBiome(final int x, final int z) {
        return getParent().getBiome(x, z);
    }

    @Override
    default BlockState getBlock(final int x, final int y, final int z) {
        return getParent().getBlock(x, y, z);
    }

    @Override
    default BaseBlock getFullBlock(final int x, final int y, final int z) {
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    default void init(final IQueueExtent extent, final int X, final int Z) {
        getParent().init(extent, X, Z);
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
    default boolean trim(final boolean aggressive) {
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
    default void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, MutableBlockVector3 unitialized, MutableBlockVector3 unitialized2) {
        getParent().filterBlocks(filter, block, region, unitialized, unitialized2);
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
    }

    default <T extends IChunk> T findParent(final Class<T> clazz) {
        IChunk root = getParent();
        if (clazz.isAssignableFrom(root.getClass())) return (T) root;
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
            if (clazz.isAssignableFrom(root.getClass())) return (T) root;
        }
        return null;
    }
}
