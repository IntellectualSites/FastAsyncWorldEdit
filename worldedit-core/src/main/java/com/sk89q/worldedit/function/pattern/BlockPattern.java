package com.sk89q.worldedit.function.pattern;

<<<<<<< HEAD
import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockState;
=======
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.world.block.BlockStateHolder;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @deprecated Just use BaseBlock directly
 */
@Deprecated
public class BlockPattern implements Pattern {

    private BlockStateHolder block;

    public BlockPattern(BlockStateHolder block) {
        this.block = block;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        return block;
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
<<<<<<< HEAD
=======

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        return block;
    }

>>>>>>> 399e0ad5... Refactor vector system to be cleaner
}
