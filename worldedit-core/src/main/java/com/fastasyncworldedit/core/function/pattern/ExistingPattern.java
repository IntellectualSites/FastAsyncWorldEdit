package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class ExistingPattern extends AbstractExtentPattern {

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     */
    public ExistingPattern(Extent extent) {
        super(extent);
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return getExtent().getFullBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (set == get || set.equals(get)) {
            return false;
        }
        return set.setFullBlock(extent, get.getFullBlock(extent));
    }

}
