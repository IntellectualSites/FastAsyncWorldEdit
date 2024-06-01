package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class WallMask extends AbstractMask {

    private final int min;
    private final int max;
    private final Mask mask;
    private final MutableBlockVector3 vector;

    public WallMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
        this.vector = new MutableBlockVector3();
    }

    @Override
    public boolean test(BlockVector3 bv) {
        vector.setComponents(bv);
        int count = 0;
        double x = vector.x();
        double y = vector.y();
        double z = vector.z();
        vector.mutX(x + 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutX(x);
            return true;
        }
        vector.mutX(x - 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutX(x);
            return true;
        }
        vector.mutX(x);
        vector.mutZ(z + 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutZ(z);
            return true;
        }
        vector.mutZ(z - 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutZ(z);
            return true;
        }
        vector.mutZ(z);
        return count >= min && count <= max;
    }

    @Override
    public Mask copy() {
        return new WallMask(mask.copy(), min, max);
    }

}
