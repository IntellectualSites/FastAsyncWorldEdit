package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency.
 */
public class AdjacentAny2DMask extends AbstractMask {

    private final Mask mask;
    private final MutableBlockVector3 mutable;

    public AdjacentAny2DMask(Mask mask) {
        this(mask, true);
    }

    public AdjacentAny2DMask(Mask mask, boolean cache) {
        this.mask = cache ? CachedMask.cache(mask) : mask;
        mutable = new MutableBlockVector3();
    }

    @Override
    public boolean test(BlockVector3 v) {
        int x = v.x();
        int y = v.y();
        int z = v.z();
        if (mask.test(mutable.setComponents(x + 1, y, z))) {
            return true;
        }
        if (mask.test(mutable.setComponents(x - 1, y, z))) {
            return true;
        }
        if (mask.test(mutable.setComponents(x, y, z + 1))) {
            return true;
        }
        return mask.test(mutable.setComponents(x, y, z - 1));
    }

    public boolean test(Extent extent, BlockVector3 v) {
        AbstractExtentMask extentMask = (AbstractExtentMask) mask;
        int x = v.x();
        int y = v.y();
        int z = v.z();
        if (extentMask.test(extent, mutable.setComponents(x + 1, y, z))) {
            return true;
        }
        if (extentMask.test(extent, mutable.setComponents(x - 1, y, z))) {
            return true;
        }
        if (extentMask.test(extent, mutable.setComponents(x, y, z + 1))) {
            return true;
        }
        return extentMask.test(extent, mutable.setComponents(x, y, z - 1));
    }

    @Override
    public Mask copy() {
        return new AdjacentAny2DMask(mask.copy(), false);
    }

}
