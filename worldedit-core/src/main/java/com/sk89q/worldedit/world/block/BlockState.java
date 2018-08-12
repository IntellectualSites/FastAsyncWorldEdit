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

import com.boydti.fawe.object.string.MutableCharSequence;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockStateMask;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable class that represents the state a block can be in.
 */
@SuppressWarnings("unchecked")
public class BlockState implements BlockStateHolder<BlockState> {

    private final int internalId;

    // TODO FIXME have field for BlockType & propertyId (to avoid all the bit shifting / masking)
    protected BlockState(int internalId) {
        this.internalId = internalId;
    }

    /**
     * Returns a temporary BlockState for a given internal id
     * @param combinedId
     * @deprecated magic number
     * @return BlockState
     */
    @Deprecated
    public static BlockState get(int combinedId) {
        return BlockTypes.getFromStateId(combinedId).withStateId(combinedId);
    }

    /**
     * Returns a temporary BlockState for a given type and string
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(String state) {
        return get(null, state);
    }

    /**
     * Returns a temporary BlockState for a given type and string
     *  - It's faster if a BlockType is provided compared to parsing the string
     * @param type BlockType e.g. BlockTypes.STONE (or null)
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(@Nullable BlockType type, String state) {
        return get(type, state, 0);
    }

    /**
     * Returns a temporary BlockState for a given type and string
     *  - It's faster if a BlockType is provided compared to parsing the string
     * @param type BlockType e.g. BlockTypes.STONE (or null)
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(@Nullable BlockType type, String state, int propId) {
        int propStrStart = state.indexOf('[');
        if (type == null) {
            CharSequence key;
            if (propStrStart == -1) {
                key = state;
            } else {
                MutableCharSequence charSequence = MutableCharSequence.getTemporal();
                charSequence.setString(state);
                charSequence.setSubstring(0, propStrStart);
                key = charSequence;
            }
            type = BlockTypes.get(key);
        }
        if (propStrStart == -1) {
            return type.getDefaultState();
        }

        List<? extends Property> propList = type.getProperties();

        MutableCharSequence charSequence = MutableCharSequence.getTemporal();
        charSequence.setString(state);

        if (propList.size() == 1) {
            AbstractProperty property = (AbstractProperty) propList.get(0);
            String name = property.getName();

            charSequence.setSubstring(propStrStart + name.length() + 2, state.length() - 1);

            return type.withPropertyId(property.getIndexFor(charSequence));
        }

        int stateId = type.getInternalId() + (propId << BlockTypes.BIT_OFFSET);
        int length = state.length();
        AbstractProperty property = null;

        int last = propStrStart + 1;
        for (int i = last; i < length; i++) {
            char c = state.charAt(i);
            switch (c) {
                case ']':
                case ',': {
                    charSequence.setSubstring(last, i);
                    int index = property.getIndexFor(charSequence);
                    stateId = property.modifyIndex(stateId, index);
                    last = i + 1;
                    break;
                }
                case '=': {
                    charSequence.setSubstring(last, i);
                    property = (AbstractProperty) type.getPropertyMap().get(charSequence);
                    last = i + 1;
                    break;
                }
                default:
                    continue;
            }
        }
        return type.withPropertyId(stateId >> BlockTypes.BIT_OFFSET);
    }

    @Override
    public BlockState withPropertyId(int propertyId) {
        return getBlockType().withPropertyId(propertyId);
    }

    @Override
    public Mask toMask(Extent extent) {
        return new SingleBlockStateMask(extent, this);
    }

    @Override
    public boolean apply(Extent extent, Vector get, Vector set) throws WorldEditException {
        return extent.setBlock(set, this);
    }

    @Override
    public BlockState apply(Vector position) {
        return this;
    }

    @Deprecated
    public int getInternalId() {
        return this.internalId;
    }

    @Override
    public boolean hasNbtData() {
        return getNbtData() != null;
    }

    @Override
    public String getNbtId() {
        return "";
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        return null;
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        throw new UnsupportedOperationException("This class is immutable.");
    }

    /**
     * The internal id with no type information
     * @return
     */
    @Deprecated
    @Override
    public final int getInternalPropertiesId() {
        return this.getInternalId() >> BlockTypes.BIT_OFFSET;
    }

    @Override
    public final BlockTypes getBlockType() {
        return BlockTypes.get(this.getInternalId() & BlockTypes.BIT_MASK);
    }

    @Deprecated
    @Override
    public final int getInternalBlockTypeId() {
        return this.getInternalId() & BlockTypes.BIT_MASK;
    }

    @Override
    public <V> BlockState with(final Property<V> property, final V value) {
        try {
            int newState = ((AbstractProperty) property).modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? new BlockState(newState) : this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        }
    }

    @Override
    public <V> BlockState with(final PropertyKey property, final V value) {
        try {
            int newState = ((AbstractProperty) getBlockType().getProperty(property)).modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? new BlockState(newState) : this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        }
    }

    @Override
    public final <V> V getState(final Property<V> property) {
        try {
            AbstractProperty ap = (AbstractProperty) property;
            return (V) ap.getValue(this.getInternalId());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        }
    }

    @Deprecated
    @Override
    public final <V> V getState(final PropertyKey key) {
        return getState(getBlockType().getProperty(key));
    }

    @Override
    @Deprecated
    public final Map<Property<?>, Object> getStates() {
        BlockType type = this.getBlockType();
        // Lazily initialize the map
        Map<? extends Property, Object> map = Maps.asMap(type.getPropertiesSet(), (Function<Property, Object>) input -> getState(input));
        return (Map<Property<?>, Object>) map;
    }

    /**
     * Deprecated, use masks - not try to this fuzzy/non fuzzy state nonsense
     * @return
     */
    @Deprecated
    public BlockState toFuzzy() {
        return this;
    }

    @Override
    public int hashCode() {
        return getInternalId();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    @Deprecated
    public boolean equalsFuzzy(BlockStateHolder o) {
        try {
            return o.getInternalId() == this.getInternalId();
        } catch (ClassCastException e) {
            // Shouldn't happen unless something modifies WorldEdit
            e.printStackTrace();
        }
        if (!getBlockType().equals(o.getBlockType())) {
            return false;
        }

        Set<Property> differingProperties = new HashSet<>();
        for (Object state : o.getStates().keySet()) {
            if (getState((Property) state) == null) {
                differingProperties.add((Property) state);
            }
        }
        for (Property property : getStates().keySet()) {
            if (o.getState(property) == null) {
                differingProperties.add(property);
            }
        }

        for (Property property : getStates().keySet()) {
            if (differingProperties.contains(property)) {
                continue;
            }
            if (!Objects.equals(getState(property), o.getState(property))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BlockState toImmutableState() {
        return this;
    }

    @Override
    public String toString() {
        return getAsString();
    }
}





























