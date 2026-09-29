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

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.sk89q.worldedit.blocks.Blocks.resolveProperties;

public class StateApplyingPattern extends AbstractExtentPattern {

    private final Map<String, String> states;
    //FAWE - avoid race conditions, faster property applications
    private final Map<BlockType, PropertyApplication[]> cache = new ConcurrentHashMap<>();
    //FAWE end

    public StateApplyingPattern(Extent extent, Map<String, String> statesToSet) {
        super(extent);
        this.states = statesToSet;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        //FAWE start
        BlockState block = position.getBlock(getExtent());
        final BlockType blockType = block.getBlockType();
        // weak consistency is enough, no need to use computeIfAbsent
        PropertyApplication[] applications = cache.get(blockType);
        if (applications == null) {
            applications = resolvePropertiesFor(blockType);
            cache.put(blockType, applications);
        }
        for (PropertyApplication entry : applications) {
            if (blockType.hasProperty(entry.property().getKey())) {
                block = block.with(entry.property(), entry.value());
            }
            //FAWE end
        }
        return block.toBaseBlock();
    }

    //FAWE start - faster property application
    private record PropertyApplication(Property<Object> property, Object value) {

    }

    private PropertyApplication[] resolvePropertiesFor(BlockType blockType) {
        Map<Property<Object>, Object> map = resolveProperties(states, blockType);
        final PropertyApplication[] applications = new PropertyApplication[map.size()];
        int i = 0;
        for (Entry<Property<Object>, Object> entry : map.entrySet()) {
            Property<Object> property = entry.getKey();
            Object o = entry.getValue();
            applications[i++] = new PropertyApplication(property, o);
        }
        return applications;
    }
    //FAWE end

}
