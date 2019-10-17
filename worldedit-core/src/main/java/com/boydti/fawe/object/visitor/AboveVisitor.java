package com.boydti.fawe.object.visitor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.object.HasFaweQueue;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Visits adjacent points on the same X-Z plane as long as the points
 * pass the given mask, and then executes the provided region
 * function on the entire column.
 * <p>
 * <p>This is used by {@code //fill}.</p>
 */
public class AboveVisitor extends RecursiveVisitor {

    private int baseY;

    /**
     * Create a new visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param baseY    the base Y
     */
    public AboveVisitor(Mask mask, RegionFunction function, int baseY) {
        this(mask, function, baseY, Integer.MAX_VALUE, null);
    }

    public AboveVisitor(Mask mask, RegionFunction function, int baseY, int depth, HasFaweQueue hasFaweQueue) {
        super(mask, function, depth);
        checkNotNull(mask);

        this.baseY = baseY;

        setDirections(
                BlockVector3.UNIT_MINUS_X,
                BlockVector3.UNIT_MINUS_Y,
                BlockVector3.UNIT_MINUS_Z,
                BlockVector3.UNIT_X,
                BlockVector3.UNIT_Y,
                BlockVector3.UNIT_Z
        );
    }

    @Override
    public boolean isVisitable(BlockVector3 from, BlockVector3 to) {
        return (from.getBlockY() >= baseY) && super.isVisitable(from, to);
    }
}
