package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public class AdjacentMask extends AbstractMask {

    private final int min;
    private final int max;
    private final Mask mask;
    private final MutableBlockVector3 vector;

    public AdjacentMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
        this.vector = new MutableBlockVector3();
    }

    @Override
    public boolean test(BlockVector3 bv) {
        vector.setComponents(bv);
        double x = bv.x();
        double y = bv.y();
        double z = bv.z();
        vector.mutX(x + 1);
        int count = 0;
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
        vector.mutY(y + 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutY(y);
            return true;
        }
        vector.mutY(y - 1);
        if (mask.test(vector) && ++count == min && max >= 8) {
            vector.mutY(y);
            return true;
        }
        vector.mutY(y);
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
        return new AdjacentMask(mask.copy(), min, max);
    }

}
