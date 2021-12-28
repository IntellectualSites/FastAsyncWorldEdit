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

/**
 * A {@link RecursiveVisitor} that goes orthogonally to the side and down, but never up.
 */
public class NonRisingVisitor extends RecursiveVisitor {

    /**
     * Create a new resursive visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @deprecated Use {@link NonRisingVisitor#NonRisingVisitor(Mask, RegionFunction, int, int, int, Extent)}
     */
    @Deprecated
    public NonRisingVisitor(Mask mask, RegionFunction function) {
        //FAWE start - int depth, y min/max
        this(mask, function, Integer.MAX_VALUE, WorldEdit
                        .getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getVersionMinY(),
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getVersionMaxY(), null
        );
        //FAWE end
    }

    //FAWE start - int depth, preloading, min/max y

    /**
     * Create a new recursive visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param depth    the maximum number of iterations
     * @param minY     minimum allowable y to visit. Inclusive.
     * @param maxY     maximum allowable y to visit. Inclusive.
     */
    public NonRisingVisitor(Mask mask, RegionFunction function, int depth, int minY, int maxY) {
        this(mask, function, Integer.MAX_VALUE, minY, maxY, null);
    }

    /**
     * Create a new recursive visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param depth    the maximum number of iterations
     * @param minY     minimum allowable y to visit. Inclusive.
     * @param maxY     maximum allowable y to visit. Inclusive.
     * @param extent   the extent for preloading
     */
    public NonRisingVisitor(Mask mask, RegionFunction function, int depth, int minY, int maxY, Extent extent) {
        super(mask, function, depth, minY, maxY, extent);
        setDirections(
                BlockVector3.UNIT_X,
                BlockVector3.UNIT_MINUS_X,
                BlockVector3.UNIT_Z,
                BlockVector3.UNIT_MINUS_Z,
                BlockVector3.UNIT_MINUS_Y
        );
    }
    //FAWE end

}
