package com.sk89q.worldedit.function.mask;

import com.fastasyncworldedit.core.function.mask.ABlockMask;
import com.fastasyncworldedit.core.function.mask.SingleBlockStateMask;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public class InverseSingleBlockStateMask extends ABlockMask {

    private final char ordinal;
    private final boolean isAir;

    public BlockState getBlockState() {
        return BlockState.getFromOrdinal(ordinal);
    }

    public InverseSingleBlockStateMask(Extent extent, BlockState state) {
        super(extent);
        isAir = state.isAir();
        this.ordinal = state.getOrdinalChar();
    }

    private InverseSingleBlockStateMask(Extent extent, char ordinal, boolean isAir) {
        super(extent);
        this.ordinal = ordinal;
        this.isAir = isAir;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int test = vector.getBlock(getExtent()).getOrdinal();
        if (isAir && test == 0) {
            return false;
        }
        return ordinal != test;
    }

    @Override
    public final boolean test(BlockState state) {
        int test = state.getOrdinalChar();
        if (isAir && test == 0) {
            return false;
        }
        return test != ordinal;
    }

    @Override
    public Mask inverse() {
        return new SingleBlockStateMask(getExtent(), BlockState.getFromOrdinal(ordinal));
    }

    @Override
    public Mask copy() {
        return new InverseSingleBlockStateMask(getExtent(), ordinal, isAir);
    }

}
