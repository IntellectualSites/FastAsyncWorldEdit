package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

/**
 * An optimized version of the {@link AdjacentMask} for single adjacency.
 */
public class AdjacentAnyMask extends AbstractMask implements ResettableMask {

    private final CachedMask mask;
    private transient MutableBlockVector3 mutable;

    public AdjacentAnyMask(Mask mask) {
        this.mask = CachedMask.cache(mask);
        mutable = new MutableBlockVector3();
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector3();
    }

    public CachedMask getParentMask() {
        return mask;
    }

    @Override
    public boolean test(BlockVector3 v) {
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

    public BlockVector3 direction(BlockVector3 v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        if (mask.test(x + 1, y, z)) {
            mutable.setComponents(1, 0, 0);
        }else
        if (mask.test(x - 1, y, z)) {
            mutable.setComponents(-1, 0, 0);
        }else
        if (mask.test(x, y, z + 1)) {
            mutable.setComponents(0, 0, 1);
        }else
        if (mask.test(x, y, z - 1)) {
            mutable.setComponents(0, 0, -1);
        }else
        if (y < 256 && mask.test(x, y + 1, z)) {
            mutable.setComponents(0, 1, 0);
        }else
        if (y > 0 && mask.test(x, y - 1, z)) {
            mutable.setComponents(0, -1, 0);
        }
        return mutable.getX() == 0 && mutable.getY() == 0 && mutable.getZ() == 0 ? null : mutable;
    }
}
