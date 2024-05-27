package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A filter is an interface used for setting blocks.
 */
public interface Filter {

//    /**
//     * Checks whether a chunk should be read.
//     *
//     * @param chunkX the x coordinate in the chunk
//     * @param chunkZ the z coordinate in the chunk
//     */
//    default boolean appliesChunk(
//            int chunkX,
//            int chunkZ
//    ) {
//        return true;
//    }

    /**
     * Do something with the IChunk<br>
     */
    default @Nonnull <T extends IChunk> T applyChunk(T chunk, @Nullable Region region) {
        return chunk;
    }

//    default boolean appliesLayer(IChunk chunk, int layer) {
//        return true;
//    }

    /**
     * Make changes to the block here<br> - e.g., block.setId(...)<br> - Note: Performance is
     * critical here<br>
     */
    default void applyBlock(FilterBlock block) {
    }

    /**
     * Do something with the IChunk after block filtering.
     */
    default void finishChunk(IChunk chunk) {
    }

    /**
     * Fork this for use by another thread - Typically filters are simple and don't need to create
     * another copy to be thread safe here
     *
     * @return this
     */
    default Filter fork() {
        return this;
    }

    default void join() {

    }

    /**
     * Signals to the filter the edit has concluded
     *
     * @since TODO
     */
    default void finish() {

    }

}
