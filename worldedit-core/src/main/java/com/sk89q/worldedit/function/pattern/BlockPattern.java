package com.sk89q.worldedit.function.pattern;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

/**
 * @deprecated Just use BaseBlock directly
 */
@Deprecated
public class BlockPattern implements Pattern {

    private BlockStateHolder block;

    public BlockPattern(BlockStateHolder block) {
        this.block = block;
    }

    /**
     * Get the block.
     *
     * @return the block that is always returned
     */
    public BlockStateHolder getBlock() {
        return block;
    }

    /**
     * Set the block that is returned.
     *
     * @param block the block
     */
    public void setBlock(BlockStateHolder block) {
        checkNotNull(block);
        this.block = block;
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        return block;
    }

}
