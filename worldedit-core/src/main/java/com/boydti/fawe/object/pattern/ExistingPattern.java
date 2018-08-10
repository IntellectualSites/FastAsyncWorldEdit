package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class ExistingPattern extends AbstractExtentPattern {
    public ExistingPattern(Extent extent) {
        super(extent);
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        return getExtent().getBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        if (set.equals(get)) {
            return false;
        }
        return extent.setBlock(set, extent.getBlock(get));
    }
}
