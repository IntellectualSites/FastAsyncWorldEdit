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

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.object.string.MutableCharSequence;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable class that represents the state a block can be in.
 */
@SuppressWarnings("unchecked")
public class BlockState implements BlockStateHolder<BlockState> {

    private final BlockType blockType;
    private final Map<Property<?>, Object> values;

    private BaseBlock emptyBaseBlock;

    BlockState(BlockType blockType) {
        this.blockType = blockType;
        this.values = new LinkedHashMap<>();
        this.emptyBaseBlock = new BaseBlock(this);
    }

    // FAWE begin
    /**
     * Returns a temporary BlockState for a given internal id
     * @param combinedId
     * @deprecated magic number
     * @return BlockState
     */
    @Deprecated
    public static BlockState getFromInternalId(int combinedId) throws InputParseException {
        return BlockTypes.getFromStateId(combinedId).withStateId(combinedId);
    }

    @Deprecated
    public static BlockState getFromOrdinal(int ordinal) {
        return BlockTypes.states[ordinal];
    }

    /**
     * Returns a temporary BlockState for a given type and string
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(String state) throws InputParseException {
        return get(null, state);
    }

    /**
     * Returns a temporary BlockState for a given type and string
     *  - It's faster if a BlockType is provided compared to parsing the string
     * @param type BlockType e.g. BlockTypes.STONE (or null)
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(@Nullable BlockType type, String state) throws InputParseException {
        return get(type, state, null);
    }

    /**
     * Returns a temporary BlockState for a given type and string
     *  - It's faster if a BlockType is provided compared to parsing the string
     * @param type BlockType e.g. BlockTypes.STONE (or null)
     * @param state String e.g. minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(@Nullable BlockType type, String state, BlockState defaultState) throws InputParseException {
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
            if (type == null) {
                String input = key.toString();
                throw new SuggestInputParseException("Does not match a valid block type: " + input, input, () -> Stream.of(BlockTypes.values)
                        .filter(b -> b.getId().contains(input))
                        .map(e1 -> e1.getId())
                        .collect(Collectors.toList())
                );
            }
        }
        if (propStrStart == -1) {
            return type.getDefaultState();
        }

        List<? extends Property> propList = type.getProperties();

        if (state.charAt(state.length() - 1) != ']') state = state + "]";
        MutableCharSequence charSequence = MutableCharSequence.getTemporal();
        charSequence.setString(state);

        if (propList.size() == 1) {
            AbstractProperty property = (AbstractProperty) propList.get(0);
            String name = property.getName();

            charSequence.setSubstring(propStrStart + name.length() + 2, state.length() - 1);

            return type.withPropertyId(property.getIndexFor(charSequence));
        }
        int stateId;
        if (defaultState != null) {
            stateId = defaultState.getInternalId();
        } else {
            stateId = type.getInternalId();
        }
        int length = state.length();
        AbstractProperty property = null;

        int last = propStrStart + 1;
        for (int i = last; i < length; i++) {
            char c = state.charAt(i);
            switch (c) {
                case ']':
                case ',': {
                    charSequence.setSubstring(last, i);
                    if (property != null) {
                        int index = property.getIndexFor(charSequence);
                        if (index == -1) {
                            String input = charSequence.toString();
                            List<Object> values = property.getValues();
                            throw new SuggestInputParseException("No value: " + input + " for " + type, input, () ->
                                values.stream()
                                .map(v -> v.toString())
                                .filter(v -> v.startsWith(input))
                                .collect(Collectors.toList()));
                        }
                        stateId = property.modifyIndex(stateId, index);
                    } else {
                        // suggest
                        PropertyKey key = PropertyKey.get(charSequence);
                        if (key == null || !type.hasProperty(key)) {
                            // Suggest property
                            String input = charSequence.toString();
                            BlockType finalType = type;
                            throw new SuggestInputParseException("Invalid property " + type + " | " + input, input, () ->
                                finalType.getProperties().stream()
                                .map(p -> p.getName())
                                .filter(p -> p.startsWith(input))
                                .collect(Collectors.toList()));
                        } else {
                            throw new SuggestInputParseException("No operator for " + state, "", () -> Arrays.asList("="));
                        }
                    }
                    property = null;
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
    // FAWE end

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBlock(set, this);
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return this.toBaseBlock();
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

    @Deprecated
    @Override
    public final int getInternalBlockTypeId() {
        return this.getInternalId() & BlockTypes.BIT_MASK;
    }

    @Override
    public <V> BlockState with(final Property<V> property, final V value) {
        try {
            BlockType type = getBlockType();
            int newState = ((AbstractProperty) property).modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? type.withStateId(newState) : this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        }
    }

    @Override
    public <V> BlockState with(final PropertyKey property, final V value) {
        try {
            BlockType type = getBlockType();
            int newState = ((AbstractProperty) type.getProperty(property)).modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? type.withStateId(newState) : this;
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
    public final Map<Property<?>, Object> getStates() {
        return Collections.unmodifiableMap(this.values);
    }

    @Override
    public BaseBlock toBaseBlock() {
        return this.emptyBaseBlock;
    }

    @Override
    public BaseBlock toBaseBlock(CompoundTag compoundTag) {
        if (compoundTag == null) {
            return toBaseBlock();
        }
        return new BaseBlock(this, compoundTag);
    }

    /**
     * Internal method used for creating the initial BlockState.
     *
     * Sets a value. DO NOT USE THIS.
     *
     * @param property The state
     * @param value The value
     * @return The blockstate, for chaining
     */
    BlockState setState(final Property<?> property, final Object value) {
        this.values.put(property, value);
        return this;
    }

    @Override
    public BlockType getBlockType() {
    	return this.blockType;
    }

    @Override
    public boolean equalsFuzzy(BlockStateHolder<?> o) {
        if (this == o) {
            // Added a reference equality check for
            return true;
        }
        if (!getBlockType().equals(o.getBlockType())) {
            return false;
        }

        Set<Property<?>> differingProperties = new HashSet<>();
        for (Object state : o.getStates().keySet()) {
            if (getState((Property<?>) state) == null) {
                differingProperties.add((Property<?>) state);
            }
        }
        for (Property<?> property : getStates().keySet()) {
            if (o.getState(property) == null) {
                differingProperties.add(property);
            }
        }

        for (Property<?> property : getStates().keySet()) {
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

	@Override
	public int getInternalId() {
		return blockType.getInternalId();
	}

	@Override
	public BlockMaterial getMaterial() {
		return blockType.getMaterial();
	}

	@Override
	public int getOrdinal() {
		//?
		return 0;
	}
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockState)) {
            return false;
        }
        return equalsFuzzy((BlockState) obj);
    }

    private Integer hashCodeCache = null;

    @Override
    public int hashCode() {
        if (hashCodeCache == null) {
            hashCodeCache = Objects.hash(blockType, values);
        }
        return hashCodeCache;
    }

}
