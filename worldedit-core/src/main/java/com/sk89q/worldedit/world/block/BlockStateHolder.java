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

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public interface BlockStateHolder<B extends BlockStateHolder<B>> extends FawePattern, TileEntityBlock {

    @Override
    default BaseBlock apply(BlockVector3 position) {
        return this.toBaseBlock();
    }

    /**
     * Get the block type
     *
     * @return The type
     */
    BlockType getBlockType();

    /**
     * Magic number (legacy uses)
     * @param propertyId
     * @return
     */
    @Deprecated
    B withPropertyId(int propertyId);

    /**
     * Get combined id (legacy uses)
     * @return
     */
    @Deprecated
    int getInternalId();

    @Deprecated
    int getOrdinal();

    @Deprecated
    char getOrdinalChar();

    BlockMaterial getMaterial();
    /**
     * Get type id (legacy uses)
     * @return
     */
    @Deprecated
    int getInternalBlockTypeId();

    /**
     * Get the block data (legacy uses)
     * @return
     */
    @Deprecated
    int getInternalPropertiesId();

    /**
     * Returns a BlockState with the given state and value applied.
     *
     * @param property The state
     * @param value The value
     * @return The modified state, or same if could not be applied
     */
    <V> B with(final Property<V> property, final V value);

    /**
     * Returns a BlockStateHolder with the given state and value applied.
     *
     * @param property The property key
     * @param value The value
     * @return The modified state, or same if could not be applied
     */
    <V> B with(final PropertyKey property, final V value);

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
     * Checks if the type is the same, and if the matched states are the same.
     *
     * @param o other block
     * @return true if equal
     */
    boolean equalsFuzzy(BlockStateHolder<?> o);

    /**
     * Returns an immutable {@link BlockState} from this BlockStateHolder.
     *
     * @return A BlockState
     */
    BlockState toImmutableState();

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @return The BaseBlock
     */
    BaseBlock toBaseBlock();

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @param compoundTag The NBT Data to apply
     * @return The BaseBlock
     */
    BaseBlock toBaseBlock(CompoundTag compoundTag);

    /**
     * Return the name of the title entity ID.
     *
     * @return tile entity ID, non-null string
     */
    default String getNbtId() {
        return "";
    }

    /**
     * Returns whether the block contains NBT data. {@link #getNbtData()}
     * must not return null if this method returns true.
     *
     * @return true if there is NBT data
     */
    default boolean hasNbtData() {
        return false;
    }

    /**
     * Get the object's NBT data (tile entity data). The returned tag, if
     * modified in any way, should be sent to {@link #setNbtData(CompoundTag)}
     * so that the instance knows of the changes. Making changes without
     * calling {@link #setNbtData(CompoundTag)} could have unintended
     * consequences.
     *
     * <p>{@link #hasNbtData()} must return true if and only if method does
     * not return null.</p>
     *
     * @return compound tag, or null
     */
    @Nullable
    default CompoundTag getNbtData() {
        return null;
    }

    /**
     * Set the object's NBT data (tile entity data).
     *
     * @param nbtData NBT data, or null if no data
     */
    default void setNbtData(@Nullable CompoundTag nbtData) {
        throw new UnsupportedOperationException("State is immutable");
    }

    default String getAsString() {
        if (getStates().isEmpty()) {
            return this.getBlockType().getId();
        } else {
            String properties = getStates().entrySet().stream()
                .map(entry -> entry.getKey().getName()
                    + "="
                    + entry.getValue().toString().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
            return this.getBlockType().getId() + "[" + properties + "]";
        }
    }
}
