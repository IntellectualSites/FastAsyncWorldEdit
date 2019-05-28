package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.math.BlockVector3;

public class AdjacentAnyMask2 extends AbstractMask {
    @Override
    public boolean test(BlockVector3 vector) {
        return false;
    }
}
