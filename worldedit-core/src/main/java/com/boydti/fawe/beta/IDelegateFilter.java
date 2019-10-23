package com.boydti.fawe.beta;

import com.sk89q.worldedit.regions.Region;
import javax.annotation.Nullable;

public interface IDelegateFilter extends Filter {

    Filter getParent();

    @Override
    default boolean appliesChunk(int chunkX, int chunkZ) {
        return getParent().appliesChunk(chunkX, chunkZ);
    }

    @Override
    default IChunk applyChunk(IChunk chunk, @Nullable Region region) {
        return getParent().applyChunk(chunk, region);
    }

    @Override
    default boolean appliesLayer(IChunk chunk, int layer) {
        return getParent().appliesLayer(chunk, layer);
    }

    @Override
    default void applyBlock(FilterBlock block) {
        getParent().applyBlock(block);
    }

    @Override
    default void finishChunk(IChunk chunk) {
        getParent().finishChunk(chunk);
    }

    @Override
    default void join() {
        getParent().join();
    }

    @Override
    default Filter fork() {
        Filter fork = getParent().fork();
        if (fork != getParent()) {
            return newInstance(fork);
        }
        return this;
    }

    Filter newInstance(Filter other);
}