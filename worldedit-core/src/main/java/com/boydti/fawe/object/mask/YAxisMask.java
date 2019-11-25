package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.math.BlockVector3;

public class YAxisMask extends AbstractMask implements ResettableMask {

    private transient int layer = -1;

    @Override
    public boolean test(BlockVector3 vector) {
        if (layer == -1) {
            layer = vector.getBlockY();
        }
        return vector.getBlockY() == layer;
    }

    @Override
    public void reset() {
        this.layer = -1;
    }

}
