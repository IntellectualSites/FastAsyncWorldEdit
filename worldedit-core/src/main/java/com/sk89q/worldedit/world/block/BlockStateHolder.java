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

package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;

import java.util.Map;
import java.util.stream.Collectors;

public interface BlockStateHolder<T extends BlockStateHolder> extends FawePattern, TileEntityBlock {

    /**
     * Get the block type
     *
     * @return The type
     */
    BlockTypes getBlockType();

    /**
     * Magic number (legacy uses)
     * @param propertyId
     * @return
     */
    @Deprecated
    default BlockStateHolder withPropertyId(int propertyId) {
        return getBlockType().withPropertyId(propertyId);
    }

    /**
     * Get combined id (legacy uses)
     * @return
     */
    @Deprecated
    int getInternalId();

    @Deprecated
    int getOrdinal();

    default BlockMaterial getMaterial() {
        return getBlockType().getMaterial();
    }

    /**
     * Get type id (legacy uses)
     * @return
     */
    @Deprecated
    default int getInternalBlockTypeId() {
        return getBlockType().getInternalId();
    }

    /**
     * Get the block data (legacy uses)
     * @return
     */
    @Deprecated
    int getInternalPropertiesId();

    Mask toMask(Extent extent);

    /**
     * Returns a BlockStateHolder with the given state and value applied.
     *
     * @param property The state
     * @param value The value
     * @return The modified state, or same if could not be applied
     */
    <V> T with(final Property<V> property, final V value);

    /**
     * Returns a BlockStateHolder with the given state and value applied.
     *
     * @param property The property key
     * @param value The value
     * @return The modified state, or same if could not be applied
     */
    <V> BlockStateHolder with(final PropertyKey property, final V value);

    /**
     * Gets the value at the given state
     *
     * @param property The state
     * @return The value
     */
    <V> V getState(Property<V> property);

    /**
     * Gets the value at the given state
     *
     * @param property The state
     * @return The value
     */
    <V> V getState(final PropertyKey property);

    /**
     * Gets an immutable collection of the states.
     *
     * @return The states
     */
    Map<Property<?>, Object> getStates();

    /**
     * @deprecated use masks - not try to this fuzzy/non fuzzy state nonsense
     * @param o other block
     * @return true if equal
     */
    @Deprecated
    boolean equalsFuzzy(BlockStateHolder o);

    /**
     * Returns an immutable BlockStateHolder from this BlockStateHolder.
     *
     * @return A BlockState
     */
    BlockState toImmutableState();

    default String getAsString() {
        if (getStates().isEmpty()) {
            return this.getBlockType().getId();
        } else {
            String properties = getStates().entrySet().stream().map(entry -> entry.getKey().getName() + "=" + entry.getValue().toString().toLowerCase()).collect(Collectors.joining(","));
            return this.getBlockType().getId() + "[" + properties + "]";
        }
    }
}
