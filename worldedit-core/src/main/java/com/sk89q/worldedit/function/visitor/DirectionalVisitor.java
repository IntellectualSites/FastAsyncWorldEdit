/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visits adjacent points on the same X-Z plane as long as the points
 * pass the given mask, and then executes the provided region
 * function on the entire column.
 * <p>
 * <p>This is used by {@code //fill}.</p>
 */
public class DirectionalVisitor extends RecursiveVisitor {

    private final BlockVector3 origin;
    private final BlockVector3 dirVec;

    public DirectionalVisitor(Mask mask, RegionFunction function, BlockVector3 origin, BlockVector3 direction) {
        this(mask, function, origin, direction, Integer.MAX_VALUE);
    }

    public DirectionalVisitor(Mask mask, RegionFunction function, BlockVector3 origin, BlockVector3 direction, int distance) {
        super(mask, function, distance);
        checkNotNull(mask);
        this.origin = origin;
        this.dirVec = direction;

        setDirections(
            BlockVector3.at(1, 0, 0),
            BlockVector3.at(-1, 0, 0),
            BlockVector3.at(0, 0, 1),
            BlockVector3.at(0, 0, -1),
            BlockVector3.at(0, -1, 0),
            BlockVector3.at(0, 1, 0)
        );
    }

    @Override
    public boolean isVisitable(final BlockVector3 from, final BlockVector3 to) {
        int dx = to.getBlockX() - from.getBlockX();
        int dz = to.getBlockZ() - from.getBlockZ();
        int dy = to.getBlockY() - from.getBlockY();

        if (dx != 0) {
            if (dirVec.getBlockX() != 0 && dirVec.getBlockX() != dx) {
                return false;
            }
        }
        if (dy != 0) {
            if (dirVec.getBlockY() != 0 && dirVec.getBlockY() != dy) {
                return false;
            }
        }
        if (dz != 0) {
            if (dirVec.getBlockZ() != 0 && dirVec.getBlockZ() != dz) {
                return false;
            }
        }
        return super.isVisitable(from, to);
    }
}