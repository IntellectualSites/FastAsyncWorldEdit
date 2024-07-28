package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

public class CachedMask extends AbstractDelegateMask implements ResettableMask {

    private final boolean hasExtent;
    private transient BlockVector3Set cache_checked;
    private transient BlockVector3Set cache_results;

    public CachedMask(Mask mask) {
        this(mask, false);
    }

    /**
     * Create a new CachedMask instance for the given mask
     *
     * @param mask  Mask to cache results of
     * @param local If the area will be small
     * @since 2.4.0
     */
    public CachedMask(Mask mask, boolean local) {
        super(mask);
        hasExtent = mask instanceof AbstractExtentMask;
        if (local) {
            cache_checked = new LocalBlockVectorSet();
            cache_results = new LocalBlockVectorSet();
        } else {
            cache_checked = new BlockVectorSet();
            cache_results = new BlockVectorSet();
        }
    }

    public static CachedMask cache(Mask mask) {
        if (mask instanceof CachedMask) {
            return (CachedMask) mask;
        }
        return new CachedMask(mask);
    }

    @Override
    public void reset() {
        cache_checked = new BlockVectorSet();
        cache_results = new BlockVectorSet();
        resetCache();
    }

    private void resetCache() {
        cache_checked.clear();
        cache_results.clear();
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int x = vector.x();
        int y = vector.y();
        int z = vector.z();
        try {
            boolean check = cache_checked.add(x, y, z);
            if (!check) {
                return cache_results.contains(x, y, z);
            }
            boolean result = getMask().test(vector);
            if (result) {
                cache_results.add(x, y, z);
            }
            return result;
        } catch (UnsupportedOperationException ignored) {
            boolean result = getMask().test(vector);
            resetCache();
            cache_checked.add(x, y, z);
            if (result) {
                cache_results.add(x, y, z);
            }
            return result;
        }
    }

    public boolean test(@Nullable Extent extent, BlockVector3 vector) {
        if (!hasExtent || !(extent instanceof AbstractExtentMask)) {
            return test(vector);
        }
        int x = vector.x();
        int y = vector.y();
        int z = vector.z();
        AbstractExtentMask mask = (AbstractExtentMask) getMask();
        try {
            boolean check = cache_checked.add(x, y, z);
            if (!check) {
                return cache_results.contains(x, y, z);
            }
            boolean result = mask.test(extent, vector);
            if (result) {
                cache_results.add(x, y, z);
            }
            return result;
        } catch (UnsupportedOperationException ignored) {
            boolean result = mask.test(extent, vector);
            resetCache();
            cache_checked.add(x, y, z);
            if (result) {
                cache_results.add(x, y, z);
            }
            return result;
        }
    }

    @Override
    public Mask copy() {
        return new CachedMask(getMask().copy(), cache_checked instanceof LocalBlockVectorSet);
    }

}
