package com.boydti.fawe.function.mask;

import com.boydti.fawe.object.mask.ResettableMask;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.math.BlockVector3;

public class XAxisMask extends AbstractMask implements ResettableMask {

    private transient int layer = -1;

    public XAxisMask(Extent extent) {

    }

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
