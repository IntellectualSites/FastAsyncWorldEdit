package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

/**
 * Restricts the
 */
public class XAxisMask extends AbstractMask implements ResettableMask {

    private transient int layer = -1;

    @Override
    public boolean test(BlockVector3 vector) {
        if (layer == -1) {
            layer = vector.getBlockX();
        }
        return vector.getBlockX() == layer;
    }

    @Override
    public void reset() {
        this.layer = -1;
    }

}
