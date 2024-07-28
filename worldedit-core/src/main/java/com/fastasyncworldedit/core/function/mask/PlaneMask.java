package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class PlaneMask extends AbstractMask implements ResettableMask {

    private transient int mode = -1;
    private transient int originX = Integer.MAX_VALUE;
    private transient int originY = Integer.MAX_VALUE;
    private transient int originZ = Integer.MAX_VALUE;

    @Override
    public boolean test(BlockVector3 vector) {
        switch (mode) {
            case -1:
                originX = vector.x();
                originY = vector.y();
                originZ = vector.z();
                mode = 0;
                return true;
            case 0:
            case 1:
            case 2:
            case 4:
                int original = mode;
                if (originX != vector.x()) {
                    mode &= 1;
                }
                if (originY != vector.y()) {
                    mode &= 2;
                }
                if (originZ != vector.z()) {
                    mode &= 4;
                }
                if (Integer.bitCount(mode) >= 3) {
                    mode = original;
                    return false;
                }
            default:
                if (originX != vector.x() && (mode & 1) == 0) {
                    return false;
                }
                if (originZ != vector.z() && (mode & 4) == 0) {
                    return false;
                }
                return originY == vector.y() || (mode & 2) != 0;

        }
    }

    @Override
    public void reset() {
        mode = -1;
    }

    @Override
    public Mask copy() {
        return new PlaneMask();
    }

}
