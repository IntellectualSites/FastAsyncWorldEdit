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

package com.sk89q.worldedit.function;

import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.implementation.filter.block.FilterBlock;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Performs a function on points in a region.
 */
//FAWE start - extends Filter
public interface RegionFunction extends Filter {
//FAWE end

    /**
     * Apply the function to the given position.
     *
     * @param position the position
     * @return true if something was changed
     * @throws WorldEditException thrown on an error
     */
    boolean apply(BlockVector3 position) throws WorldEditException;


    //FAWE start
    @Override
    default void applyBlock(FilterBlock block) {
        apply(block);
    }
    //FAWE end
}
