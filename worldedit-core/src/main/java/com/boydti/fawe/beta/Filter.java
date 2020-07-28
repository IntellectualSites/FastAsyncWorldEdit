package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;
import com.sk89q.worldedit.regions.Region;
import org.jetbrains.annotations.Range;

import javax.annotation.Nullable;

/**
 * A filter is an interface used for setting blocks.
 */
public interface Filter {

    /**
     * Checks whether a chunk should be read.
     *
     * @param chunkX the x coordinate in the chunk
     * @param chunkZ the z coordinate in the chunk
     */
    default boolean appliesChunk(@Range(from = 0, to = 15) int chunkX,
        @Range(from = 0, to = 15) int chunkZ) {
        return true;
    }

    /**
     * Do something with the IChunk.
     *
     * @return return null if you don't want to filter blocks, otherwise return the chunk
     */
    default <T extends IChunk> T applyChunk(T chunk, @Nullable Region region) {
        return chunk;
    }

    default boolean appliesLayer(IChunk chunk, int layer) {
        return true;
    }

    /**
     * Make changes to the block here<.
     * br> - e.g., block.setId(...)<br> - Note: Performance is
     * critical here<br>
     *
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
     * another copy to be thread safe here.
     *
     * @return this
     */
    default Filter fork() {
        return this;
    }

    default void join() {

    }
}
