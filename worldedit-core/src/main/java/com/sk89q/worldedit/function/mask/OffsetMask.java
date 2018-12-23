package com.sk89q.worldedit.function.mask;

<<<<<<< HEAD
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
=======
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;

>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether another mask tests true for a position that is offset
 * a given vector.
 */
public class OffsetMask extends AbstractMask {

    private Mask mask;
<<<<<<< HEAD
    private Vector offset;
    private MutableBlockVector mutable = new MutableBlockVector();
=======
    private BlockVector3 offset;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner

    /**
     * Create a new instance.
     *
     * @param mask   the mask
     * @param offset the offset
     */
    public OffsetMask(Mask mask, BlockVector3 offset) {
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
    public BlockVector3 getOffset() {
        return offset;
    }

    /**
     * Set the offset.
     *
     * @param offset the offset
     */
    public void setOffset(BlockVector3 offset) {
        checkNotNull(offset);
        this.offset = offset;
    }

    @Override
<<<<<<< HEAD
    public boolean test(Vector vector) {
        mutable.mutX((vector.getX() + offset.getX()));
        mutable.mutY((vector.getY() + offset.getY()));
        mutable.mutZ((vector.getZ() + offset.getZ()));
        return getMask().test(mutable);
=======
    public boolean test(BlockVector3 vector) {
        return getMask().test(vector.add(offset));
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        Mask2D childMask = getMask().toMask2D();
        if (childMask != null) {
            return new OffsetMask2D(childMask, getOffset().toBlockVector2());
        } else {
            return null;
        }
    }


}
