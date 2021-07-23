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

package com.sk89q.worldedit.internal.block;

import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;

public final class BlockStateIdAccess {

    //FAWE start - Register via ordinal ID
    private static final int INVALID_ID = -1;

    /**
     * An invalid internal ID, for verification purposes.
     * @return an internal ID which is never valid
     */
    public static int invalidId() {
        return INVALID_ID;
    }

    public static boolean isValidInternalId(int internalId) {
        return internalId != INVALID_ID;
    }

    public static int getBlockStateId(BlockState holder) {
        return holder.getInternalId();
    }

    @Nullable
    public static BlockState getBlockStateById(int id) {
        return BlockState.getFromInternalId(id);
    }

    private BlockStateIdAccess() {
    }
    //FAWE end

}
