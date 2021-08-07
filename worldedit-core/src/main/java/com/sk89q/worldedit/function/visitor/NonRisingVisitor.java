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

import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * A {@link RecursiveVisitor} that goes orthogonally to the side and down, but never up.
 */
public class NonRisingVisitor extends RecursiveVisitor {

    //FAWE start - int depth

    /**
     * Create a new recursive visitor.
     *
     * @param mask     the mask
     * @param function the function
     * @param depth    max number of recursions
     * @param minY     min applicable y
     * @param maxY     max applicable y
     */
    public NonRisingVisitor(Mask mask, RegionFunction function, int depth, int minY, int maxY) {
        super(mask, function, depth, minY, maxY);
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
