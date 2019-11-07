package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;
import com.sk89q.worldedit.regions.Region;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A filter is an interface used for setting blocks.
 */
public interface Filter {

    /**
     * Checks whether a chunk should be read.
     *
     * @param chunkX the x coordinate in the chunk
     * @param chunkZ the z coordinate in the chunk
     * @return
     */
    default boolean appliesChunk(@Range(from = 0, to = 15) int chunkX,
        @Range(from = 0, to = 15) int chunkZ) {
        return true;
    }

    /**
     * Do something with the IChunk<br> - Return null if you don't want to filter blocks<br> -
     * Return the chunk if you do want to filter blocks<br>
     *
     * @param chunk
     * @return
     */
    default <T extends IChunk> T applyChunk(T chunk, @Nullable Region region) {
        return chunk;
    }

    default boolean appliesLayer(IChunk chunk, int layer) {
        return true;
    }

    /**
     * Make changes to the block here<br> - e.g. block.setId(...)<br> - Note: Performance is
     * critical here<br>
     *
     * @param block
     */
    default void applyBlock(FilterBlock block) {
    }

    /**
     * Do something with the IChunk after block filtering.
     *
     * @param chunk
     * @return
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
}
