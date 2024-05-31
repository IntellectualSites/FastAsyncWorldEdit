package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Mask that tests adjacency only in 2D/the same y-level
 *
 * @since TODO
 */
public class Adjacent2DMask extends AbstractMask {

    private final int min;
    private final int max;
    private final Mask mask;
    private final MutableBlockVector3 vector;

    /**
     * Mask that tests adjacency only in 2D/the same y-level
     *
     * @param mask Mask required to be adjacent
     * @param requiredMin Minimum number of positive adjacency matches required
     * @param requiredMax Maximum number of positive adjacency matches  required
     * @since TODO
     */
    public Adjacent2DMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
        this.vector = new MutableBlockVector3();
    }

    @Override
    public boolean test(BlockVector3 bv) {
        vector.setComponents(bv);
        double x = bv.x();
        double z = bv.z();
        vector.mutX(x + 1);
        int count = 0;
        if (mask.test(vector) && ++count == min && max >= 4) {
            return true;
        }
        vector.mutX(x - 1);
        if (mask.test(vector) && ++count == min && max >= 4) {
            return true;
        }
        vector.mutX(x);
        vector.mutZ(z + 1);
        if (mask.test(vector) && ++count == min && max >= 4) {
            return true;
        }
        vector.mutZ(z - 1);
        if (mask.test(vector) && ++count == min && max >= 4) {
            return true;
        }
        return count >= min && count <= max;
    }

    @Override
    public Mask copy() {
        return new Adjacent2DMask(mask.copy(), min, max);
    }

}
