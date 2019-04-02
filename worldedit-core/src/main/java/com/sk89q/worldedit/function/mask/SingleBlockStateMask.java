package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class SingleBlockStateMask extends AbstractExtentMask {
    private final BlockStateHolder state;

    public BlockStateHolder getBlockState() {
        return state;
    }

    public SingleBlockStateMask(Extent extent, BlockStateHolder state) {
        super(extent);
        this.state = state;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return state.equals(getExtent().getBlock(vector));
    }

    @Override
    public Mask inverse() {
        return new BlockMaskBuilder().add(state).build(getExtent()).inverse();
    }
}
