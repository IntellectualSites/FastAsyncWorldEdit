/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visits adjacent points on the same X-Z plane as long as the points
 * pass the given mask, and then executes the provided region
 * function on the entire column.
 *
 * <p>This is used by {@code //fill}.</p>
 */
public class DownwardVisitor extends RecursiveVisitor {

    private final int baseY;

    /**
     * Create a new visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param baseY    the base Y
     * @deprecated Use {@link DownwardVisitor#DownwardVisitor(Mask, RegionFunction, int, int, int, int)}
     */
    @Deprecated
    public DownwardVisitor(Mask mask, RegionFunction function, int baseY) {
        //FAWE start - int depth, min/max y
        this(mask, function, baseY, Integer.MAX_VALUE, WorldEdit
                        .getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMinY(),
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMaxY(), null
        );
        //FAWE end
    }

    //FAWE start - int depth, min/max y, preloading

    /**
     * Create a new visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param baseY    the base Y
     * @param depth    maximum number of iterations
     * @param minY     minimum allowable y to visit. Inclusive.
     * @param maxY     maximum allowable y to visit. Inclusive.
     */
    public DownwardVisitor(Mask mask, RegionFunction function, int baseY, int depth, int minY, int maxY) {
        this(mask, function, baseY, depth, minY, maxY, null);
    }

    /**
     * Create a new visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param baseY    the base Y
     * @param depth    maximum number of iterations
     * @param minY     minimum allowable y to visit. Inclusive.
     * @param maxY     maximum allowable y to visit. Inclusive.
     * @param extent   extent for preloading
     */
    public DownwardVisitor(Mask mask, RegionFunction function, int baseY, int depth, int minY, int maxY, Extent extent) {
        super(mask, function, depth, minY, maxY, extent);
        checkNotNull(mask);

        this.baseY = baseY;

        setDirections(
                BlockVector3.UNIT_X,
                BlockVector3.UNIT_MINUS_X,
                BlockVector3.UNIT_Z,
                BlockVector3.UNIT_MINUS_Z,
                BlockVector3.UNIT_MINUS_Y
        );
    }
    //FAWE end

    @Override
    protected boolean isVisitable(BlockVector3 from, BlockVector3 to) {
        int fromY = from.y();
        return (fromY == baseY || to.subtract(from).y() < 0) && super.isVisitable(from, to);
    }

}
