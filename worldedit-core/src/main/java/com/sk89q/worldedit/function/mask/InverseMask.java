package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

public class InverseMask extends AbstractMask {
    private final Mask mask;

    public InverseMask(Mask other) {
        this.mask = other;
    }

    @Override
    public boolean test(BlockVector3 pos) {
        return !mask.test(pos);
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

    @Override
    public Mask copy() {
        return new InverseMask(mask.copy());
    }

    @Override
    public boolean replacesAir() {
        return mask.replacesAir();
    }
}
