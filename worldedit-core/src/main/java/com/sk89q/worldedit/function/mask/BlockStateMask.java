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

import com.google.common.collect.Maps;
import com.sk89q.worldedit.blocks.Blocks;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class BlockStateMask extends AbstractExtentMask {

    private final Map<String, String> states;
    private final boolean strict;
    private final Map<BlockType, Map<Property<Object>, Object>> cache = Maps.newHashMap();

    /**
     * Creates a mask that checks if a given block has the desired properties set to the desired value.
     *
     * @param extent the extent to get blocks from
     * @param states the desired states (property -> value) that a block should have to match the mask
     * @param strict true to only match blocks that have all properties and values, false to also match blocks that
     *               do not have the properties (but only fail blocks with the properties but wrong values)
     */
    public BlockStateMask(Extent extent, Map<String, String> states, boolean strict) {
        super(extent);
        this.states = states;
        this.strict = strict;
    }

    //FAWE start
    @Override
    public boolean test(BlockVector3 vector) {
        return test(vector.getBlock(getExtent()));
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(extent.getBlock(vector));
    }

    public boolean test(BlockState block) {
        final Map<Property<Object>, Object> checkProps = cache
                .computeIfAbsent(block.getBlockType(), (b -> Blocks.resolveProperties(states, b)));
        if (strict && checkProps.isEmpty()) {
            return false;
        }
        return checkProps.entrySet().stream()
                .allMatch(entry -> block.getState(entry.getKey()) == entry.getValue());
    }

    @Override
    public boolean replacesAir() {
        return test(BlockTypes.AIR.getDefaultState()) || test(BlockTypes.CAVE_AIR.getDefaultState()) || test(BlockTypes.VOID_AIR.getDefaultState());
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    @Override
    public Mask copy() {
        Map<String, String> statesClone = new HashMap<>();
        states.forEach(statesClone::put);
        return new BlockStateMask(getExtent(), statesClone, strict);
    }
    //FAWE end
}
