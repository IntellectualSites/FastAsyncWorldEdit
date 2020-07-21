package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

public class WallMask extends AbstractMask {
    private final int min;
    private final int max;
    private final Mask mask;
    private MutableBlockVector3 blockVector3;

    public WallMask(Mask mask, int requiredMin, int requiredMax) {
        this.mask = mask;
        this.min = requiredMin;
        this.max = requiredMax;
        this.blockVector3 = new MutableBlockVector3();
    }

    @Override
    public boolean test(Extent extent, BlockVector3 bv) {
        blockVector3.setComponents(bv);
        int count = 0;
        double x = blockVector3.getX();
        double y = blockVector3.getY();
        double z = blockVector3.getZ();
        blockVector3.mutX(x + 1);
        if (mask.test(extent, blockVector3) && ++count == min && max >= 8) {
            blockVector3.mutX(x);
            return true;
        }
        blockVector3.mutX(x - 1);
        if (mask.test(extent, blockVector3) && ++count == min && max >= 8) {
            blockVector3.mutX(x);
            return true;
        }
        blockVector3.mutX(x);
        blockVector3.mutZ(z + 1);
        if (mask.test(extent, blockVector3) && ++count == min && max >= 8) {
            blockVector3.mutZ(z);
            return true;
        }
        blockVector3.mutZ(z - 1);
        if (mask.test(extent, blockVector3) && ++count == min && max >= 8) {
            blockVector3.mutZ(z);
            return true;
        }
        blockVector3.mutZ(z);
        return count >= min && count <= max;
    }
}
