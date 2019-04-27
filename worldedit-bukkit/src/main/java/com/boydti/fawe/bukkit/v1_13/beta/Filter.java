package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.block.BaseBlock;

public class Filter {
    /**
     * Check whether a chunk should be read
     *
     * @param cx
     * @param cz
     * @return
     */
    public boolean appliesChunk(int cx, int cz) {
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
    public IChunk applyChunk(IChunk chunk) {
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
    public void applyBlock(int x, int y, int z, BaseBlock block) {
    }

    /**
     * Do something with the IChunk after block filtering<br>
     *
     * @param chunk
     * @return
     */
    public void finishChunk(IChunk chunk) {
    }
}
