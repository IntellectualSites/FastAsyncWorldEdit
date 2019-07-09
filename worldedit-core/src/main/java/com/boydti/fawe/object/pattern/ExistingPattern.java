package com.boydti.fawe.object.pattern;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class ExistingPattern extends AbstractExtentPattern {
    public ExistingPattern(Extent extent) {
        super(extent);
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
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
