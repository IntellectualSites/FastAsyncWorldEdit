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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class SolidBlockMask extends AbstractExtentMask {
    // FAWE start - precompute solid blocks
    private static final boolean[] SOLID = initialize();

    private static boolean[] initialize() {
        final boolean[] solid = new boolean[BlockTypesCache.states.length];
        for (int i = 0; i < solid.length; i++) {
            solid[i] = BlockTypesCache.states[i].getBlockType().getMaterial().isMovementBlocker();
        }
        return solid;
    }
    // FAWE end

    public SolidBlockMask(Extent extent) {
        super(extent);
    }

    // FAWE start
    @Override
    public boolean test(final Extent extent, final BlockVector3 position) {
        final int ordinal = position.getOrdinal(extent);
        return SOLID[ordinal];
    }

    @Override
    public boolean test(final BlockVector3 vector) {
        return test(getExtent(), vector);
    }

    /**
     * {@return whether the given block state is considered solid by this mask}
     * @since 2.13.0
     */
    public static boolean isSolid(BlockState blockState) {
        return SOLID[blockState.getOrdinal()];
    }


    @Override
    public Mask copy() {
        return new SolidBlockMask(getExtent());
    }
    //FAWE end

}
