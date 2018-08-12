package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether another mask tests true for a position that is offset
 * a given vector.
 */
public class OffsetMask extends AbstractMask {

    private Mask mask;
    private Vector offset;
    private MutableBlockVector mutable = new MutableBlockVector();

    /**
     * Create a new instance.
     *
     * @param mask   the mask
     * @param offset the offset
     */
    public OffsetMask(Mask mask, Vector offset) {
        checkNotNull(mask);
        checkNotNull(offset);
        this.mask = mask;
        this.offset = offset;
    }

    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Set the mask.
     *
     * @param mask the mask
     */
    public void setMask(Mask mask) {
        checkNotNull(mask);
        this.mask = mask;
    }

    /**
     * Get the offset.
     *
     * @return the offset
     */
    public Vector getOffset() {
        return offset;
    }

    /**
     * Set the offset.
     *
     * @param offset the offset
     */
    public void setOffset(Vector offset) {
        checkNotNull(offset);
        this.offset = offset;
    }

    @Override
    public boolean test(Vector vector) {
        mutable.mutX((vector.getX() + offset.getX()));
        mutable.mutY((vector.getY() + offset.getY()));
        mutable.mutZ((vector.getZ() + offset.getZ()));
        return getMask().test(mutable);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        Mask2D childMask = getMask().toMask2D();
        if (childMask != null) {
            return new OffsetMask2D(childMask, getOffset().toVector2D());
        } else {
            return null;
        }
    }

    public static Class<?> inject() {
        return OffsetMask.class;
    }
}
