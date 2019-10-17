package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class InverseSingleBlockStateMask extends ABlockMask {
    private final char ordinal;

    public BlockStateHolder getBlockState() {
        return BlockState.getFromOrdinal(ordinal);
    }

    public InverseSingleBlockStateMask(Extent extent, BlockState state) {
        super(extent);
        this.ordinal = state.getOrdinalChar();
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return ordinal != vector.getOrdinal(getExtent());
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getOrdinalChar() != ordinal;
    }

    @Override
    public Mask inverse() {
        return new SingleBlockStateMask(getExtent(), BlockState.getFromOrdinal(ordinal));
    }
}
