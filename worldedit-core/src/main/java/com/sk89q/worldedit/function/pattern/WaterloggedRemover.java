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

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Removes the waterlogged state from blocks if possible. If not possible, returns air.
 */
public class WaterloggedRemover extends AbstractExtentPattern {

    private static SoftReference<BlockState[]> cache = new SoftReference<>(null);

    private synchronized BlockState[] getRemap() {
        BlockState[] remap = this.cache.get();
        if (remap != null) return remap;
        this.cache = new SoftReference<>(remap = new BlockState[BlockTypes.states.length]);

        // init
        for (int i = 0; i < remap.length; i++) {
            BlockState state = remap[i];
            BlockType type = state.getBlockType();
            if (!type.hasProperty(PropertyKey.WATERLOGGED)) {
                continue;
            }
            if (state.getState(PropertyKey.WATERLOGGED) == Boolean.TRUE) {
                remap[i] = state.with(PropertyKey.WATERLOGGED, false);
            }
        }
        return remap;
    }

    private final BlockState[] remap;

    public WaterloggedRemover(Extent extent) {
        super(extent);
        this.remap = getRemap();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BaseBlock block = getExtent().getFullBlock(position);
        BlockState newState = remap[block.getOrdinal()];
        if (newState != null) {
            return newState.toBaseBlock(block.getNbtData());
        }
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }
}