package com.sk89q.worldedit.function.mask;

import javax.annotation.Nullable;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class InverseMask extends AbstractMask {
    private final Mask mask;

    public InverseMask(Mask other) {
        this.mask = other;
    }

    @Override
    public boolean test(Extent extent, BlockVector3 pos) {
        return !mask.test(extent, pos);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        Mask2D mask2d = mask.toMask2D();
        if (mask2d != null) {
            return Masks.negate(mask2d);
        } else {
            return null;
        }
    }

    @Override
    public Mask inverse() {
        return mask;
    }
}
