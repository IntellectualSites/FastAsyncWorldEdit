package com.boydti.fawe.object.mask;

import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;

public class CachedMask extends AbstractDelegateMask implements ResettableMask {

    private transient MutableBlockVector mutable = new MutableBlockVector();
    private transient LocalBlockVectorSet cache_checked = new LocalBlockVectorSet();
    private transient LocalBlockVectorSet cache_results = new LocalBlockVectorSet();

    public CachedMask(Mask mask) {
        super(mask);
        cache_checked.setOffset(Integer.MIN_VALUE, Integer.MIN_VALUE);
        cache_results.setOffset(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public static CachedMask cache(Mask mask) {
        if (mask instanceof CachedMask) {
            return (CachedMask) mask;
        }
        return new CachedMask(mask);
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector();
        cache_checked = new LocalBlockVectorSet();
        cache_results = new LocalBlockVectorSet();
        resetCache();
    }

    private void resetCache() {
        cache_checked.clear();
        cache_results.clear();
        cache_checked.setOffset(Integer.MIN_VALUE, Integer.MIN_VALUE);
        cache_results.setOffset(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public boolean test(int x, int y, int z) {
        try {
            boolean check = cache_checked.add(x, y, z);
            if (!check) {
                return cache_results.contains(x, y, z);
            }
            boolean result = getMask().test(mutable.setComponents(x, y, z));
            if (result) cache_results.add(x, y, z);
            return result;
        } catch (UnsupportedOperationException ignore) {
            boolean result = getMask().test(mutable.setComponents(x, y, z));
            if (y < 0 || y > 255) return result;
            resetCache();
            cache_checked.setOffset(x, z);
            cache_results.setOffset(x, z);
            cache_checked.add(x, y, z);
            if (result) cache_results.add(x, y, z);
            return result;
        }
    }
}
