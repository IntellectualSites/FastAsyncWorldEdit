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

    private BaseBlock block;

    /**
     * Create a new pattern with the given block.
     *
     * @param block the block
     */
    public BlockPattern(BlockStateHolder<?> block) {
        setBlock(block);
    }

    /**
     * Get the block.
     *
     * @return the block that is always returned
     */
    public BaseBlock getBlock() {
        return block;
    }

    /**
     * Set the block that is returned.
     *
     * @param block the block
     */
    public void setBlock(BlockStateHolder<?> block) {
        checkNotNull(block);
        this.block = block.toBaseBlock();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return block;
    }

}
