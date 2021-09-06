package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency.
 */
public class AdjacentAnyMask extends AbstractMask implements ResettableMask {

    private final CachedMask mask;
    private final MutableBlockVector3 mutable;
    private final int minY;
    private final int maxY;

    public AdjacentAnyMask(Mask mask, int minY, int maxY) {
        this.mask = CachedMask.cache(mask);
        mutable = new MutableBlockVector3();
        this.minY = minY;
        this.maxY = maxY;
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
        } else if (y < maxY && mask.test(x, y + 1, z)) {
            return mutable.setComponents(0, 1, 0);
        } else if (y > minY && mask.test(x, y - 1, z)) {
            return mutable.setComponents(0, -1, 0);
        } else {
            return null;
        }
    }

    @Override
    public Mask copy() {
        return new AdjacentAnyMask(mask.copy(), minY, maxY);
    }

}
