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

package com.sk89q.worldedit.world.registry;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Provides information on blocks and provides methods to create them.
 */
public interface BlockRegistry {

    /**
     * Gets the name for the given block.
     *
     * @param blockType the block
     * @return The name
     */
    Component getRichName(BlockType blockType);

    /**
     * Gets the name for the given block.
     *
     * @param blockType the block
     * @return The name, or null if it's unknown
     * @deprecated Names are now translatable, use {@link #getRichName(BlockType)}.
     */
    @Deprecated
    @Nullable
    default String getName(BlockType blockType) {
        return getRichName(blockType).toString();
    }

    /**
     * Get the material for the given block.
     *
     * @param blockType the block
     * @return the material, or null if the material information is not known
     */
    @Nullable
    BlockMaterial getMaterial(BlockType blockType);

    //FAWE start
    @Nullable
    default BlockMaterial getMaterial(BlockState state) {
        return getMaterial(state.getBlockType());
    }
    //FAWE end

    /**
     * Get an unmodifiable map of states for this block.
     *
     * @param blockType the block
     * @return a map of states where the key is the state's ID
     */
    Map<String, ? extends Property<?>> getProperties(BlockType blockType);

    /**
     * Retrieve the internal ID for a given state, if possible.
     *
     * @param state The block state
     * @return the internal ID of the state
     */
    OptionalInt getInternalBlockStateId(BlockState state);

    //FAWE start

    /**
     * Get a collection of all blocks in their default state as string representations.
     */
    default Collection<String> values() {
        return Collections.emptyList();
    }

    /**
     * Get an unmodifiable map of all block properties
     *
     * @return a map of states where the key is the property's ID
     */
    Map<String, ? extends List<Property<?>>> getAllProperties();
    //FAWE end
}
