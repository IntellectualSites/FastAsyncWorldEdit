package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

public class AdjacentMask extends AbstractMask {
    private final int min, max;
    private final Mask mask;
    private MutableBlockVector3 v;

    public AdjacentMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
        this.v = new MutableBlockVector3();
    }

    @Override
    public boolean test(BlockVector3 bv) {
    	v.setComponents(bv);
        int count = 0;
        double x = bv.getX();
        double y = bv.getY();
        double z = bv.getZ();
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
