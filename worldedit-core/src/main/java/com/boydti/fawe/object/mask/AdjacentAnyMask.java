package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency
 */
public class AdjacentAnyMask extends AbstractMask implements ResettableMask {

    private final CachedMask mask;
    private transient MutableBlockVector mutable = new MutableBlockVector();

    public AdjacentAnyMask(Mask mask) {
        this.mask = CachedMask.cache(mask);
        mutable = new MutableBlockVector();
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector();
    }

    public CachedMask getParentMask() {
        return mask;
    }

    @Override
    public boolean test(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        if (mask.test(x + 1, y, z)) {
            return true;
        }
        if (mask.test(x - 1, y, z)) {
            return true;
        }
        if (mask.test(x, y, z + 1)) {
            return true;
        }
        if (mask.test(x, y, z - 1)) {
            return true;
        }
        if (y < 256 && mask.test(x, y + 1, z)) {
            return true;
        }
        if (y > 0 && mask.test(x, y - 1, z)) {
            return true;
        }
        return false;
    }

    public Vector direction(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        if (mask.test(x + 1, y, z)) {
            return mutable.setComponents(1, 0, 0);
        }
        if (mask.test(x - 1, y, z)) {
            return mutable.setComponents(-1, 0, 0);
        }
        if (mask.test(x, y, z + 1)) {
            return mutable.setComponents(0, 0, 1);
        }
        if (mask.test(x, y, z - 1)) {
            return mutable.setComponents(0, 0, -1);
        }
        if (y < 256 && mask.test(x, y + 1, z)) {
            return mutable.setComponents(0, 1, 0);
        }
        if (y > 0 && mask.test(x, y - 1, z)) {
            return mutable.setComponents(0, -1, 0);
        }
        return null;
    }
}