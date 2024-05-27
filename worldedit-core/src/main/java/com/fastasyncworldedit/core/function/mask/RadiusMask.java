package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class RadiusMask extends AbstractMask implements ResettableMask {

    private final int minSqr;
    private final int maxSqr;
    private transient BlockVector3 pos;

    public RadiusMask(int min, int max) {
        this.minSqr = min * min;
        this.maxSqr = max * max;
    }

    private RadiusMask(Integer minSqr, Integer maxSqr) {
        this.minSqr = minSqr;
        this.maxSqr = maxSqr;
    }

    @Override
    public void reset() {
        pos = null;
    }

    @Override
    public boolean test(BlockVector3 to) {
        if (pos == null) {
            pos = to.toImmutable();
        }
        int dx = pos.x() - to.x();
        int d = dx * dx;
        if (d > maxSqr) {
            return false;
        }
        int dz = pos.z() - to.z();
        d += dz * dz;
        if (d > maxSqr) {
            return false;
        }
        int dy = pos.y() - to.y();
        d += dy * dy;
        return d >= minSqr && d <= maxSqr;
    }

    @Override
    public Mask copy() {
        return new RadiusMask((Integer) minSqr, (Integer) maxSqr);
    }

}
