package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

/**
 * Restricts the
 */
public class PlaneMask extends AbstractMask implements ResettableMask {

    private transient int mode = -1;
    private transient int originX = Integer.MAX_VALUE, originY = Integer.MAX_VALUE, originZ = Integer.MAX_VALUE;

    @Override
    public boolean test(Vector vector) {
        switch (mode) {
            case -1:
                originX = vector.getBlockX();
                originY = vector.getBlockY();
                originZ = vector.getBlockZ();
                mode = 0;
                return true;
            case 0:
            case 1:
            case 2:
            case 4:
                int original = mode;
                if (originX != vector.getBlockX()) {
                    mode &= 1;
                }
                if (originY != vector.getBlockY()) {
                    mode &= 2;
                }
                if (originZ != vector.getBlockZ()) {
                    mode &= 4;
                }
                if (Integer.bitCount(mode) >= 3) {
                    mode = original;
                    return false;
                }
            default:
                if (originX != vector.getBlockX() && (mode & 1) == 0) {
                    return false;
                }
                if (originZ != vector.getBlockZ() && (mode & 4) == 0) {
                    return false;
                }
                if (originY != vector.getBlockY() && (mode & 2) == 0) {
                    return false;
                }
                return true;

        }
    }

    @Override
    public void reset() {
        mode = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
