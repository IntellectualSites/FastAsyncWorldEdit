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

package com.sk89q.worldedit.function.pattern;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.NonAbstractForCompatibility;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;

/**
 * Returns a {@link BaseBlock} for a given position.
 */
//FAWE start - extends Filter
public interface Pattern extends Filter {

    /**
     * Return a {@link BaseBlock} for the given position.
     *
     * @param position the position
     * @return a block
     * @see NonAbstractForCompatibility This must be overridden by new subclasses.
     * @deprecated use {@link Pattern#applyBlock(BlockVector3)}
     */
    @Deprecated
    @NonAbstractForCompatibility(
            delegateName = "applyBlock",
            delegateParams = {BlockVector3.class}
    )
    default BaseBlock apply(BlockVector3 position) {
        return applyBlock(position);
    }

    default boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setFullBlock(extent, applyBlock(get));
    }

    @Override
    default void applyBlock(final FilterBlock block) {
        apply(block, block, block);
    }

    //FAWE end

    /**
     * Return a {@link BaseBlock} for the given position.
     *
     * @param position the position
     * @return a block
     */
    BaseBlock applyBlock(BlockVector3 position);

    /**
     * Get the likely maximum size of the volume this pattern will affect
     *
     * @return Pattern size
     * @since TODO
     */
    default BlockVector3 size() {
        return BlockVector3.ONE;
    }

}
