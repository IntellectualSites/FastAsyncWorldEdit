package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class ZAxisMask extends AbstractMask implements ResettableMask {

    private transient int layer = -1;

    public ZAxisMask() {
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (layer == -1) {
            layer = vector.z();
        }
        return vector.z() == layer;
    }

    @Override
    public void reset() {
        this.layer = -1;
    }

    @Override
    public Mask copy() {
        return new ZAxisMask();
    }

}
