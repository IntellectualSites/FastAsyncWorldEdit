package com.boydti.fawe.object.visitor;

import com.boydti.fawe.object.HasFaweQueue;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
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

        Collection<Vector> directions = getDirections();
        directions.clear();
        directions.add(new Vector(1, 0, 0));
        directions.add(new Vector(-1, 0, 0));
        directions.add(new Vector(0, 0, 1));
        directions.add(new Vector(0, 0, -1));
        directions.add(new Vector(0, 1, 0));
        directions.add(new Vector(0, -1, 0));
    }

    @Override
    public boolean isVisitable(Vector from, Vector to) {
        return (from.getBlockY() >= baseY) && super.isVisitable(from, to);
    }
}