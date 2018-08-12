package com.sk89q.worldedit.patterns;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * @deprecated See {@link com.sk89q.worldedit.function.pattern.Pattern}
 */
@Deprecated
public interface Pattern {

    /**
     * Get a block for a position. This return value of this method does
     * not have to be consistent for the same position.
     *
     * @param position the position where a block is needed
     * @return a block
     */
    public BaseBlock next(Vector position);

    /**
     * Get a block for a position. This return value of this method does
     * not have to be consistent for the same position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a block
     */
    public BaseBlock next(int x, int y, int z);

}