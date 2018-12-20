package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;

public class AdjacentMask extends AbstractMask {
    private final int min, max;
    private final Mask mask;

    public AdjacentMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
    }

    @Override
    public boolean test(Vector v) {
        int count = 0;
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();
        v.mutX(x + 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutX(x);
            return true;
        }
        v.mutX(x - 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutX(x);
            return true;
        }
        v.mutX(x);
        v.mutY(y + 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutY(y);
            return true;
        }
        v.mutY(y - 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutY(y);
            return true;
        }
        v.mutY(y);
        v.mutZ(z + 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z - 1);
        if (mask.test(v) && ++count == min && max >= 8) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z);
        return count >= min && count <= max;
    }
}
