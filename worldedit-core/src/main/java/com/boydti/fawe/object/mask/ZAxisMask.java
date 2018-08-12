package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

/**
 * Restricts the
 */
public class ZAxisMask extends AbstractMask implements ResettableMask {

    private transient int layer = -1;

    @Override
    public boolean test(Vector vector) {
        if (layer == -1) {
            layer = vector.getBlockZ();
        }
        return vector.getBlockZ() == layer;
    }

    @Override
    public void reset() {
        this.layer = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
