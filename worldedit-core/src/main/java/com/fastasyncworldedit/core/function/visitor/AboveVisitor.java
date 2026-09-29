package com.fastasyncworldedit.core.function.visitor;

import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visits adjacent points on the same X-Z plane as long as the points pass the given mask, and then
 * executes the provided region function on the entire column.
 * <p>This is used by {@code //fill}.</p>
 */
public class AboveVisitor extends RecursiveVisitor {

    private final int baseY;

    /**
     * Create a new visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param baseY    the base Y
     * @param depth    maximum number of iterations
     * @param minY     min visitable y value. Inclusive.
     * @param maxY     max visitable y value. Inclusive.
     */
    public AboveVisitor(Mask mask, RegionFunction function, int baseY, int depth, int minY, int maxY) {
        super(mask, function, depth, minY, maxY);
        checkNotNull(mask);

        this.baseY = baseY;

        setDirections(BlockVector3.UNIT_MINUS_X, BlockVector3.UNIT_MINUS_Y,
                BlockVector3.UNIT_MINUS_Z, BlockVector3.UNIT_X, BlockVector3.UNIT_Y,
                BlockVector3.UNIT_Z
        );
    }

    @Override
    public boolean isVisitable(BlockVector3 from, BlockVector3 to) {
        return (from.y() >= baseY) && super.isVisitable(from, to);
    }

}
