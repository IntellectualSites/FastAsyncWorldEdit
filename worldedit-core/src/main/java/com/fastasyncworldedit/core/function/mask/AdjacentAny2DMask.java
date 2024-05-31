package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Optimized version of {@link Adjacent2DMask} for testing for any single adjacency
 *
 * @since TODO
 */
public class AdjacentAny2DMask extends AbstractMask {

    private final Mask mask;
    private final MutableBlockVector3 mutable;

    /**
     * Optimized version of {@link Adjacent2DMask} for testing for any single adjacency. Caches results of the adjacent mask
     *
     * @param mask Mask required to be adjacent
     * @since TODO
     */
    public AdjacentAny2DMask(Mask mask) {
        this(mask, true);
    }

    /**
     * Optimized version of {@link Adjacent2DMask} for testing for any single adjacency
     *
     * @param mask  Mask required to be adjacent
     * @param cache If the result of the adjacency mask should be cached
     * @since TODO
     */
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

    /**
     * Test this mask for the given extent
     *
     * @param extent   extent to test in
     * @param position position to test at
     * @since TODO
     */
    public boolean test(Extent extent, BlockVector3 position) {
        if (!(mask instanceof AbstractExtentMask extentMask)) {
            throw new UnsupportedOperationException("Adjacency mask must inherit from AbstractExtentMask");
        }
        int x = position.x();
        int y = position.y();
        int z = position.z();
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
