package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class RadiusMask extends AbstractMask implements ResettableMask {

    private transient Vector pos;
    private final int minSqr, maxSqr;

    public RadiusMask(int min, int max) {
        this.minSqr = min * min;
        this.maxSqr = max * max;
    }

    @Override
    public void reset() {
        pos = null;
    }

    @Override
    public boolean test(Vector to) {
        if (pos == null) {
            pos = new MutableBlockVector(to);
        }
        int dx = pos.getBlockX() - to.getBlockX();
        int d = dx * dx;
        if (d > maxSqr) {
            return false;
        }
        int dz = pos.getBlockZ() - to.getBlockZ();
        d += dz * dz;
        if (d > maxSqr) {
            return false;
        }
        int dy = pos.getBlockY() - to.getBlockY();
        d += dy * dy;
        if (d < minSqr || d > maxSqr) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
