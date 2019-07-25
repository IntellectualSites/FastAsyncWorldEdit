package com.boydti.fawe.object.pattern;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class DataPattern extends AbstractExtentPattern {
    private final Pattern pattern;

    public DataPattern(Extent extent, Pattern parent) {
        super(extent);
        checkNotNull(parent);
        this.pattern = parent;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BaseBlock oldBlock = getExtent().getFullBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        return oldBlock.toBlockState().withProperties(newBlock.toBlockState()).toBaseBlock(newBlock.getNbtData());
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BaseBlock oldBlock = get.getFullBlock(extent);
        BaseBlock newBlock = pattern.apply(get);

        BlockState oldState = oldBlock.toBlockState();
        BlockState newState = oldState.withProperties(newBlock.toBlockState());
        if (newState != oldState) {
            if (oldBlock.hasNbtData()) {
                set.setFullBlock(extent, newState.toBaseBlock(oldBlock.getNbtData()));
            } else {
                set.setBlock(extent, newState);
            }
            return true;
        }
        return false;
    }
}
