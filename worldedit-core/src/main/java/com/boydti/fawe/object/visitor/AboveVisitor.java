package com.boydti.fawe.object.visitor;

import com.boydti.fawe.object.HasFaweQueue;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;
import java.util.Collection;


import static com.google.common.base.Preconditions.checkNotNull;

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
        super(mask, function, depth, hasFaweQueue);
        checkNotNull(mask);

        this.baseY = baseY;

        setDirections(
            BlockVector3.at(1, 0, 0),
            BlockVector3.at(-1, 0, 0),
            BlockVector3.at(0, 0, 1),
            BlockVector3.at(0, 0, -1),
            BlockVector3.at(0, 1, 0),
            BlockVector3.at(0, -1, 0)
        );
    }

    @Override
    public boolean isVisitable(BlockVector3 from, BlockVector3 to) {
        return (from.getBlockY() >= baseY) && super.isVisitable(from, to);
    }
}