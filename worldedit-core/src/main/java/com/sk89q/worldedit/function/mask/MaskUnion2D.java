package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.math.BlockVector2;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests true if any contained mask is true, even if it just one.
 */
public class MaskUnion2D extends MaskIntersection2D {

    /**
     * Create a new union.
     *
     * @param masks a list of masks
     */
    public MaskUnion2D(Collection<Mask2D> masks) {
        super(masks);
    }

    /**
     * Create a new union.
     *
     * @param mask a list of masks
     */
    public MaskUnion2D(Mask2D... mask) {
        super(mask);
    }

    @Override
    public boolean test(BlockVector2 vector) {
        Collection<Mask2D> masks = getMasks();

        for (Mask2D mask : masks) {
            if (mask.test(vector)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Mask2D copy2D() {
        Set<Mask2D> masksCopy = masks.stream().map(Mask2D::copy2D).collect(Collectors.toSet());
        return new MaskUnion2D(masksCopy);
    }

}
