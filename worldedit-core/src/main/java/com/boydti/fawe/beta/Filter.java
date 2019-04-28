package com.boydti.fawe.beta;

import com.sk89q.worldedit.world.block.BaseBlock;

public interface Filter {
    /**
     * Check whether a chunk should be read
     *
     * @param cx
     * @param cz
     * @return
     */
    default boolean appliesChunk(final int cx, final int cz) {
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
    default IChunk applyChunk(final IChunk chunk) {
        return chunk;
    }

    /**
     * Make changes to the block here<br>
     * - e.g. block.setId(...)<br>
     * - Note: Performance is critical here<br>
     *
     * @param x
     * @param y
     * @param z
     * @param block
     */
    default void applyBlock(final int x, final int y, final int z, final BaseBlock block) {
    }

    /**
     * Do something with the IChunk after block filtering<br>
     *
     * @param chunk
     * @return
     */
    default void finishChunk(final IChunk chunk) {
    }
}
