package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;

public interface IDelegateFilter extends Filter {

    Filter getParent();

    @Override
    default <V extends IChunk> V applyChunk(V chunk, @Nullable Region region) {
        return getParent().applyChunk(chunk, region);
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

    @Override
    default void finish() {
        getParent().finish();
    }

    Filter newInstance(Filter other);

}
