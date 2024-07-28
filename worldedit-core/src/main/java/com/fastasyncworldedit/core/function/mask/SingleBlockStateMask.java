package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.InverseSingleBlockStateMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public class SingleBlockStateMask extends ABlockMask {

    private final char ordinal;
    private final boolean isAir;

    public BlockState getBlockState() {
        return BlockState.getFromOrdinal(ordinal);
    }

    public SingleBlockStateMask(Extent extent, BlockState state) {
        super(extent);
        isAir = state.isAir();
        this.ordinal = state.getOrdinalChar();
    }

    private SingleBlockStateMask(Extent extent, char ordinal, boolean isAir) {
        super(extent);
        this.ordinal = ordinal;
        this.isAir = isAir;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int test = getExtent().getBlock(vector).getOrdinal();
        return ordinal == test || isAir && test == 0;
    }

    @Override
    public final boolean test(BlockState state) {
        return state.getOrdinalChar() == ordinal;
    }

    @Override
    public Mask inverse() {
        return new InverseSingleBlockStateMask(getExtent(), BlockState.getFromOrdinal(ordinal));
    }

    @Override
    public boolean replacesAir() {
        return isAir;
    }

    @Override
    public Mask tryCombine(Mask mask) {
        if (mask instanceof ABlockMask other) {
            if (other.test(BlockState.getFromOrdinal(ordinal))) {
                return this;
            }
            return Masks.alwaysFalse();
        }
        return null;
    }

    @Override
    public Mask copy() {
        return new SingleBlockStateMask(getExtent(), ordinal, isAir);
    }

}
