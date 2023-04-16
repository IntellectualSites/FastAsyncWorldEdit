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

import com.fastasyncworldedit.core.command.SuggestInputParseException;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.function.mask.SingleBlockStateMask;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.ITileInput;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.core.util.MutableCharSequence;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.world.block.BlanketBaseBlock;
import com.fastasyncworldedit.core.world.block.CompoundInput;
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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.enginehub.linbus.tree.LinCompoundTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable class that represents the state a block can be in.
 */
@SuppressWarnings("unchecked")
public class BlockState implements BlockStateHolder<BlockState>, Pattern {

    //FAWE start
    private final int internalId;
    private final int ordinal;
    private BlockMaterial material;
    private final BaseBlock emptyBaseBlock;
    private CompoundInput compoundInput = CompoundInput.NULL;
    //FAWE end
    private final BlockType blockType;
    private final LazyReference<String> lazyStringRepresentation;

    //FAWE start
    public BlockState(BlockType blockType, int internalId, int ordinal) {
        this.blockType = blockType;
        this.internalId = internalId;
        this.ordinal = ordinal;
        this.emptyBaseBlock = new BlanketBaseBlock(this);
        this.lazyStringRepresentation = LazyReference.from(BlockStateHolder.super::getAsString);
    }

    public BlockState(BlockType blockType, int internalId, int ordinal, @Nonnull CompoundTag tile) {
        this.blockType = blockType;
        this.internalId = internalId;
        this.ordinal = ordinal;
        this.emptyBaseBlock = new BlanketBaseBlock(this, tile);
        this.lazyStringRepresentation = LazyReference.from(BlockStateHolder.super::getAsString);
    }

    /**
     * Returns a temporary BlockState for a given internal id.
     *
     * @return BlockState
     * @deprecated Magic Numbers
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
     *
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
     * @param type  BlockType e.g., BlockTypes.STONE (or null)
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
     * @param type  BlockType e.g., BlockTypes.STONE (or null)
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
                throw new SuggestInputParseException(Caption.of("fawe.error.invalid-block-type", TextComponent.of(input)), () -> Stream.of(
                                BlockTypesCache.values)
                        .map(BlockType::id)
                        .filter(id -> StringMan.blockStateMatches(input, id))
                        .sorted(StringMan.blockStateComparator(input))
                        .collect(Collectors.toList())
                );
            }
        }
        if (propStrStart == -1) {
            return type.getDefaultState();
        }

        List<? extends Property<?>> propList = type.getProperties();

        if (state.charAt(state.length() - 1) != ']') {
            state = state + "]";
        }
        MutableCharSequence charSequence = MutableCharSequence.getTemporal();
        charSequence.setString(state);

        if (propList.size() == 1) {
            AbstractProperty<?> property = (AbstractProperty<?>) propList.get(0);
            String name = property.getName();

            charSequence.setSubstring(propStrStart + name.length() + 2, state.length() - 1);
            try {
                int index = charSequence.length() <= 0 ? -1 : property.getIndexFor(charSequence);
                if (index != -1) {
                    return type.withPropertyId(index);
                }
            } catch (Exception e) {
                throw new InputParseException(Caption.of(
                        "fawe.error.invalid-block-state-property",
                        TextComponent.of(charSequence.toString()),
                        TextComponent.of(name),
                        TextComponent.of(state)
                ), e);
            }
        }
        int stateId;
        if (defaultState != null) {
            stateId = defaultState.getInternalId();
        } else {
            stateId = type.getDefaultState().getInternalId();
        }
        int length = state.length();
        AbstractProperty<?> property = null;

        int last = propStrStart + 1;
        for (int i = last; i < length; i++) {
            char c = state.charAt(i);
            switch (c) {
                case ']':
                case ',': {
                    charSequence.setSubstring(last, i);
                    if (property != null) {
                        int index;
                        try {
                            index = property.getIndexFor(charSequence);
                        } catch (Exception e) {
                            throw new InputParseException(Caption.of(
                                    "fawe.error.invalid-block-state-property",
                                    TextComponent.of(charSequence.toString()),
                                    TextComponent.of(property.getName()),
                                    TextComponent.of(state)
                            ), e);
                        }
                        if (index == -1) {
                            throw SuggestInputParseException.of(charSequence.toString(), (List<Object>) property.getValues());
                        }
                        stateId = property.modifyIndex(stateId, index);
                    } else {
                        // suggest
                        PropertyKey key = PropertyKey.getByName(charSequence);
                        if (key == null || !type.hasProperty(key)) {
                            // Suggest property
                            String input = charSequence.toString();
                            BlockType finalType = type;
                            throw new SuggestInputParseException(
                                    Caption.of("worldedit.error.parser.unknown-property", key + ":" + input, type),
                                    () ->
                                            finalType.getProperties().stream()
                                                    .map(Property::getName)
                                                    .filter(p -> StringMan.blockStateMatches(input, p))
                                                    .sorted(StringMan.blockStateComparator(input))
                                                    .collect(Collectors.toList())
                            );
                        } else {
                            throw new SuggestInputParseException(
                                    Caption.of("fawe.error.no-operator-for-input", state),
                                    () -> Collections.singletonList("=")
                            );
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
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBlock(extent, this);
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
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
            int newState = ((AbstractProperty<V>) property).modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? type.withStateId(newState) : this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "Error resolving property " + property.getName() + " for block type " + getBlockType().id() + "(nullable) value " + value,
                    e
            );
        }
    }

    @Override
    public <V> V getState(final Property<V> property) {
        try {
            AbstractProperty<V> ap = (AbstractProperty<V>) property;
            return (V) ap.getValue(this.getInternalId());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "Error resolving property " + property.getName() + " for blocktype " + getBlockType().id(),
                    e
            );
        }
    }

    @Override
    public <V> BlockState with(final PropertyKey property, final V value) {
        try {
            BlockType type = getBlockType();
            AbstractProperty<V> abstractProperty = ((AbstractProperty<V>) type.getProperty(property));
            if (abstractProperty == null) {
                return this;
            }
            int newState = abstractProperty.modify(this.getInternalId(), value);
            return newState != this.getInternalId() ? type.withStateId(newState) : this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property not found: " + property);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "Error resolving property " + property.getName() + " for block type " + getBlockType().id() + "(nullable) value " + value,
                    e
            );
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
        for (Property<?> prop : ot.getProperties()) {
            PropertyKey key = prop.getKey();
            if (blockType.hasProperty(prop)) {
                newState = newState.with(key, other.getState(key));
            }
        }
        return newState;
    }
    //FAWE end

    @Override
    @SuppressWarnings("RedundantCast")
    public Map<Property<?>, Object> getStates() {
        //FAWE end
        BlockType type = this.getBlockType();
        // Lazily initialize the map
        Map<? extends Property<?>, Object> map = Maps.asMap(
                type.getPropertiesSet(),
                (Function<Property<?>, Object>) this::getState
        );
        //This is required for compilation, etc.
        return Collections.unmodifiableMap((Map<Property<?>, Object>) map);
        //FAWE end
    }

    @Override
    public BlockType getBlockType() {
        return this.blockType;
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
        //FAWE start
        if (o.getClass() == BlockState.class) {
            return o.getOrdinal() == this.getOrdinal();
        }
        return o.equalsFuzzy(this);
        //FAWE end
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

    //FAWE start
    @Deprecated
    @Override
    public CompoundTag getNbtData() {
        return getBlockType().getMaterial().isTile() ? getBlockType().getMaterial().getDefaultTile() : null;
    }
    //FAWE end

    @Override
    public BaseBlock toBaseBlock(LazyReference<LinCompoundTag> compoundTag) {
        if (compoundTag == null) {
            return toBaseBlock();
        }
        return new BaseBlock(this, compoundTag);
    }

    //FAWE start
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
            this.material = WorldEdit
                    .getInstance()
                    .getPlatformManager()
                    .queryCapability(Capability.GAME_HOOKS)
                    .getRegistries()
                    .getBlockRegistry()
                    .getMaterial(this);
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
    public int hashCode() {
        return getOrdinal();
    }

    public boolean isAir() {
        return blockType.getMaterial().isAir();
    }

    @Override
    public BaseBlock toBaseBlock(ITileInput input, int x, int y, int z) {
        return compoundInput.get(this, input, x, y, z);
    }

    @Override
    public BaseBlock toBaseBlock(final IBlocks blocks, final int x, final int y, final int z) {
        return compoundInput.get(this, blocks, x, y, z);
    }

    //FAWE end

    @Override
    public String getAsString() {
        return lazyStringRepresentation.getValue();
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

}
