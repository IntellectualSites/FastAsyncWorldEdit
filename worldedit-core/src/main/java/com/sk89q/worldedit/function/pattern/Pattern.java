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

package com.sk89q.worldedit.function.pattern;

import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.internal.expression.runtime.Return;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Returns a {@link BlockStateHolder} for a given position.
 * @deprecated Use FawePattern
 */
@Link(clazz = UtilityCommands.class, value = "patterns")
@Deprecated
public interface Pattern extends com.sk89q.worldedit.patterns.Pattern{

    @Override
    default BaseBlock next(Vector position) {
        return new BaseBlock(apply(position));
    }

    @Override
    default BaseBlock next(int x, int y, int z) {
        return new BaseBlock(apply(new Vector(x, y, z)));
    }

    /**
     * Return a {@link BlockStateHolder} for the given position.
     *
     * @param position the position
     * @return a block
     */
    @Deprecated
    BlockStateHolder apply(Vector position);

    default boolean apply(Extent extent, Vector get, Vector set) throws WorldEditException {
        return extent.setBlock(set, apply(get));
    }
}