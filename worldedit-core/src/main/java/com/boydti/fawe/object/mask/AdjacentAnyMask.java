package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency.
 */
public class AdjacentAnyMask extends AbstractMask implements ResettableMask {

    private final CachedMask mask;
    private final MutableBlockVector3 mutable;

    public AdjacentAnyMask(Mask mask) {
        this.mask = CachedMask.cache(mask);
        mutable = new MutableBlockVector3();
    }

    @Override
    public void reset() {
        mutable.setComponents(0, 0, 0);
    }

    public CachedMask getParentMask() {
        return mask;
    }

    @Override
    public boolean test(BlockVector3 v) {
        return direction(v) != null;
    }

    public BlockVector3 direction(BlockVector3 v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        if (mask.test(x + 1, y, z)) {
            return mutable.setComponents(1, 0, 0);
        } else if (mask.test(x - 1, y, z)) {
            return mutable.setComponents(-1, 0, 0);
        } else if (mask.test(x, y, z + 1)) {
            return mutable.setComponents(0, 0, 1);
        } else if (mask.test(x, y, z - 1)) {
            return mutable.setComponents(0, 0, -1);
        } else if (y < 256 && mask.test(x, y + 1, z)) {
            return mutable.setComponents(0, 1, 0);
        } else if (y > 0 && mask.test(x, y - 1, z)) {
            return mutable.setComponents(0, -1, 0);
        } else {
            return null;
        }
    }

    @Override
    public Mask copy() {
        return new AdjacentAnyMask(mask.copy());
    }
}
