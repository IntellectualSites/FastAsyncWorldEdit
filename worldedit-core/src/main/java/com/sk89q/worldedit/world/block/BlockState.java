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

package com.sk89q.worldedit.world.block;

import com.boydti.fawe.beta.ITileInput;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.object.string.MutableCharSequence;
import com.boydti.fawe.util.StringMan;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockStateMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * An immutable class that represents the state a block can be in.
 */
@SuppressWarnings("unchecked")
public class BlockState implements BlockStateHolder<BlockState>, Pattern {
    private final int internalId;
    private final int ordinal;
    private final char ordinalChar;
    private final BlockType blockType;
    private BlockMaterial material;
    private final BaseBlock emptyBaseBlock;
    private CompoundInput compoundInput = CompoundInput.NULL;

    protected BlockState(BlockType blockType, int internalId, int ordinal) {
        this.blockType = blockType;
        this.internalId = internalId;
        this.ordinal = ordinal;
        this.ordinalChar = (char) ordinal;
        this.emptyBaseBlock = new ImmutableBaseBlock(this);
    }

    /**
     * Returns a temporary BlockState for a given internal id.
     * @deprecated Magic Numbers
     * @return BlockState
     */

    @Deprecated
    public static BlockState getFromInternalId(int combinedId) throws InputParseException {
        return BlockTypes.getFromStateId(combinedId).withStateId(combinedId);
    }

    @Deprecated
    public static BlockState getFromOrdinal(int ordinal) {
        return BlockTypesCache.states[ordinal];
    }

    /**
     * Returns a temporary BlockState for a given type and string.
     * @param state String e.g., minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(String state) throws InputParseException {
        return get(null, state);
    }

    /**
     * Returns a temporary BlockState for a given type and string.
     *
     * <p>It's faster if a BlockType is provided compared to parsing the string.</p>
     *
     * @param type BlockType e.g., BlockTypes.STONE (or null)
     * @param state String e.g., minecraft:water[level=4]
     * @return BlockState
     */
    public static BlockState get(@Nullable BlockType type, String state) throws InputParseException {
        return get(type, state, null);
    }

    /**
     * Returns a temporary BlockState for a given type and string.
     *
     * <p>It's faster if a BlockType is provided compared to parsing the string.</p>
     *
     * @param type BlockType e.g., BlockTypes.STONE (or null)
     * @param state String e.g., minecraft:water[level=4]
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
                throw new SuggestInputParseException("Does not match a valid block type: " + input, input, () -> Stream.of(BlockTypesCache.values)
                        .filter(b -> StringMan.blockStateMatches(input, b.getId()))
                        .map(BlockType::getId)
                        .sorted(StringMan.blockStateComparator(input))
                        .collect(Collectors.toList())
                );
            }
        }
        if (propStrStart == -1) {
            return type.getDefaultState();
        }

        List<? extends Property> propList = type.getProperties();

        if (state.charAt(state.length() - 1) != ']') {
            state = state + "]";
        }
        MutableCharSequence charSequence = MutableCharSequence.getTemporal();
        charSequence.setString(state);

        if (propList.size() == 1) {
            AbstractProperty property = (AbstractProperty) propList.get(0);
            String name = property.getName();

            charSequence.setSubstring(propStrStart + name.length() + 2, state.length() - 1);
            int index = charSequence.length() <= 0 ? -1 : property.getIndexFor(charSequence);
            if (index != -1) {
                return type.withPropertyId(index);
            }
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
                            throw SuggestInputParseException.of(charSequence.toString(), property.getValues());
                        }
                        stateId = property.modifyIndex(stateId, index);
                    } else {
                        // suggest
                        PropertyKey key = PropertyKey.get(charSequence);
                        if (key == null || !type.hasProperty(key)) {
                            // Suggest property
                            String input = charSequence.toString();
                            BlockType finalType = type;
                            throw new SuggestInputParseException("Invalid property " + key + ":" + input + " for type " + type, input, () ->
                                finalType.getProperties().stream()
                                .map(Property::getName)
                                .filter(p -> StringMan.blockStateMatches(input, p))
                                .sorted(StringMan.blockStateComparator(input))
                                .collect(Collectors.toList()));
                        } else {
                            throw new SuggestInputParseException("No operator for " + state, "", () -> Collections.singletonList("="));
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
        return type.withPropertyId(stateId >> BlockTypesCache.BIT_OFFSET);
    }

    @Override
    public BlockState withPropertyId(int propertyId) {
        return getBlockType().withPropertyId(propertyId);
    }

    @Override
    public BlockType getBlockType() {
        return this.blockType;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBlock(extent, this);
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return this.toBaseBlock();
    }

    public Mask toMask() {
        return new SingleBlockStateMask(new NullExtent(), this);
    }

    @Override
    public void applyTileEntity(OutputExtent output, int x, int y, int z) {

    }

    /**
     * The internal id with no type information.
     */
    @Deprecated
    @Override
    public final int getInternalPropertiesId() {
        return this.getInternalId() >> BlockTypesCache.BIT_OFFSET;
    }

    @Deprecated
    @Override
    public final int getInternalBlockTypeId() {
        return this.getInternalId() & BlockTypesCache.BIT_MASK;
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
    public <V> V getState(final Property<V> property) {
        try {
            AbstractProperty ap = (AbstractProperty) property;
            return (V) ap.getValue(this.getInternalId());
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

    public <V> BlockState withProperties(final BlockState other) {
        BlockType ot = other.getBlockType();
        if (ot == blockType) {
            return other;
        }
        if (ot.getProperties().isEmpty() || blockType.getProperties().isEmpty()) {
            return this;
        }
        BlockState newState = this;
        for (Property<?> prop: ot.getProperties()) {
            PropertyKey key = prop.getKey();
            if (blockType.hasProperty(key)) {
                newState = newState.with(key, other.getState(key));
            }
        }
        return this;
    }

    @Override
    public Map<Property<?>, Object> getStates() {
        BlockType type = this.getBlockType();
        // Lazily initialize the map
        Map<? extends Property, Object> map = Maps.asMap(type.getPropertiesSet(), (Function<Property, Object>) this::getState);
        return Collections.unmodifiableMap((Map<Property<?>, Object>) map);
    }

    @Override
    public boolean equalsFuzzy(BlockStateHolder<?> o) {
        if (null == o) {
            return false;
        }
        if (this == o) {
            // Added a reference equality check for speediness
            return true;
        }
        if (o.getClass() == BlockState.class) {
            return o.getOrdinal() == this.getOrdinal();
        }
        return o.equalsFuzzy(this);
    }

    @Override
    public BlockState toImmutableState() {
        return this;
    }

    @Override
    public BaseBlock toBaseBlock() {
        return this.emptyBaseBlock;
    }

    @Deprecated
    @Override
    public <V> V getState(PropertyKey key) {
        return getState(getBlockType().getProperty(key));
    }

    @Override
    public BaseBlock toBaseBlock(CompoundTag compoundTag) {
        if (compoundTag == null) {
            return toBaseBlock();
        }
        return new BaseBlock(this, compoundTag);
    }

    @Override
    public int getInternalId() {
        return internalId;
    }

    @Override
    public BlockMaterial getMaterial() {
        if (this.material == null) {
            if (blockType == BlockTypes.__RESERVED__) {
                return this.material = blockType.getMaterial();
            }
            this.material = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getMaterial(this);
            if (this.material.hasContainer()) {
                this.compoundInput = CompoundInput.CONTAINER;
            }
        }
        return material;
    }

    @Override
    public final int getOrdinal() {
        return this.ordinal;
    }

    @Override
    public final char getOrdinalChar() {
        return this.ordinalChar;
    }

    @Override
    public String toString() {
        return getAsString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockState)) {
            return false;
        }

        return equalsFuzzy((BlockState) obj);
    }

    @Override
    public int hashCode() {
        return getOrdinal();
    }

    public boolean isAir() {
        try {
            return material.isAir();
        } catch (NullPointerException ignored) {
            return getMaterial().isAir();
        }
    }

    @Override
    public BaseBlock toBaseBlock(ITileInput input, int x, int y, int z) {
        return compoundInput.get(this, input, x, y, z);
    }
}
