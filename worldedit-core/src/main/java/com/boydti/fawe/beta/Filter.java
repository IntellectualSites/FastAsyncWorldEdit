package com.boydti.fawe.beta;

import com.sk89q.worldedit.regions.Region;
import javax.annotation.Nullable;

/**
 * A filter is an interface used for setting blocks
 */
public interface Filter  {
    /**
     * Checks whether a chunk should be read.
     *
     * @param chunkX
     * @param chunkZ
     * @return
     */
    default boolean appliesChunk(final int chunkX, final int chunkZ) {
        return true;
    }

    /**
     * Do something with the IChunk<br>
     * - Return null if you don't want to filter blocks<br>
     * - Return the chunk if you do want to filter blocks<br>
     *
     * @param chunk
     * @return
     */
    default IChunk applyChunk(final IChunk chunk, @Nullable Region region) {
        return chunk;
    }

    default boolean appliesLayer(IChunk chunk, int layer) {
        return true;
    }

    /**
     * Make changes to the block here<br>
     * - e.g. block.setId(...)<br>
     * - Note: Performance is critical here<br>
     *
     * @param block
     */
    default void applyBlock(final FilterBlock block) {
    }

    /**
     * Do something with the IChunk after block filtering<br>
     *
     * @param chunk
     * @return
     */
    default void finishChunk(final IChunk chunk) {
    }

    /**
     * Fork this for use by another thread
     *  - Typically filters are simple and don't need to create another copy to be thread safe here
     * @return this
     */
    default Filter fork() {
        return this;
    }

    default void join() {

    }
}
