package com.boydti.fawe.object.function.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

public class AbstractDelegateMask extends AbstractMask {

    private final Mask mask;

    public AbstractDelegateMask(Mask parent) {
        this.mask = parent;
    }

    public final Mask getMask() {
        return mask;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return mask.test(vector);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return mask.toMask2D();
    }
}
