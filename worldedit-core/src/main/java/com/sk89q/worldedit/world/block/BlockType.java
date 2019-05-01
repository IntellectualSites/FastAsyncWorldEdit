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

import static com.google.common.base.Preconditions.checkArgument;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.registry.NamespacedRegistry;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockType implements FawePattern {
	private final String id;
    private final BlockTypes.Settings settings;

    private boolean initItemType;
    private ItemType itemType;

    protected BlockType(String id, int internalId, List<BlockState> states) {
        int i = id.indexOf("[");
        this.id = i == -1 ? id : id.substring(0, i);
        this.settings = new BlockTypes.Settings(this, id, internalId, states);
    }

    @Deprecated
    public int getMaxStateId() {
    	return settings.permutations;
    }

    /**
     * Gets the ID of this block.
     *
     * @return The id
     */
    public String getId() {
    	return this.id;
    }

    public String getNamespace() {
        String id = getId();
        int i = id.indexOf(':');
        return i == -1 ? "minecraft" : id.substring(0, i);
    }

    public String getResource() {
        String id = getId();
        return id.substring(id.indexOf(':') + 1);
    }

    /**
     * Gets the name of this block, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    public String getName() {
        String name = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getName(this);
        if (name == null) {
            return getId();
        } else {
            return name;
        }
    }

    @Deprecated
    public BlockState withPropertyId(int propertyId) {
        if (settings.stateOrdinals == null) return settings.defaultState;
        return BlockTypes.states[settings.stateOrdinals[propertyId]];
    }
    
    @Deprecated
    public BlockState withStateId(int internalStateId) { //
        return this.withPropertyId(internalStateId >> BlockTypes.BIT_OFFSET);
    }

    /**
     * Properties string in the form property1=foo,prop2=bar
     * @param properties
     * @return
     */
    public BlockState withProperties(String properties) { //
        int id = getInternalId();
        for (String keyPair : properties.split(",")) {
            String[] split = keyPair.split("=");
            String name = split[0];
            String value = split[1];
            AbstractProperty btp = settings.propertiesMap.get(name);
            id = btp.modify(id, btp.getValueFor(value));
        }
        return withStateId(id);
    }

    /**
     * Gets the properties of this BlockType in a key->property mapping.
     *
     * @return The properties map
     */
    @Deprecated
    public Map<String, ? extends Property<?>> getPropertyMap() {
        return this.settings.propertiesMap;
    }

    /**
     * Gets the properties of this BlockType.
     *
     * @return the properties
     */
    @Deprecated
    public List<? extends Property<?>> getProperties() {
        return this.settings.propertiesList;
    }

    @Deprecated
    public Set<? extends Property<?>> getPropertiesSet() {
        return this.settings.propertiesSet;
    }

    /**
     * Gets a property by name.
     *
     * @param name The name
     * @return The property
     */
    @Deprecated
    public <V> Property<V> getProperty(String name) {
        return (Property<V>) this.settings.propertiesMap.get(name);
    }

    public boolean hasProperty(PropertyKey key) {
        int ordinal = key.ordinal();
        return this.settings.propertiesMapArr.length > ordinal ? this.settings.propertiesMapArr[ordinal] != null : false;
    }
    
    public <V> Property<V> getProperty(PropertyKey key) {
        try {
            return (Property<V>) this.settings.propertiesMapArr[key.ordinal()];
        } catch (IndexOutOfBoundsException ignore) {
            return null;
        }
    }

    /**
     * Gets the default state of this block type.
     *
     * @return The default state
     */
    public BlockState getDefaultState() {
        return this.settings.defaultState;
    }

//    public FuzzyBlockState getFuzzyMatcher() { //
//        return new FuzzyBlockState(this);
//    }
//
//    public FuzzyBlockState getFuzzyMatcher() { //
//        return updateField(emptyFuzzy, () -> new FuzzyBlockState(this));
//    }

    /**
     * Slow
     * @return collection of states
     */
    @Deprecated
    public List<BlockState> getAllStates() {
        if (settings.stateOrdinals == null) return Collections.singletonList(getDefaultState());
        return IntStream.of(settings.stateOrdinals).filter(i -> i != -1).mapToObj(i -> BlockTypes.states[i]).collect(Collectors.toList());
    }

    /**
     * Gets a state of this BlockType with the given properties.
     *
     * @return The state, if it exists
     */
    public BlockState getState(Map<Property<?>, Object> key) { //
        int id = getInternalId();
        for (Map.Entry<Property<?>, Object> iter : key.entrySet()) {
            Property<?> prop = iter.getKey();
            Object value = iter.getValue();

            /*
             * TODO:
             * This is likely wrong. The only place this seems to currently (Dec 23 2018)
             * be invoked is via ForgeWorld, and value is a String when invoked there...
             */
            AbstractProperty btp = this.settings.propertiesMap.get(prop.getName());
            checkArgument(btp != null, "%s has no property named %s", this, prop.getName());
            id = btp.modify(id, btp.getValueFor((String)value));
        }
        return withStateId(id);
    }


    /**
     * Gets whether this block type has an item representation.
     *
     * @return If it has an item
     */
    public boolean hasItemType() {
        return getItemType() != null;
    }

    /**
     * Gets the item representation of this block type, if it exists.
     *
     * @return The item representation
     */
    @Nullable
    public ItemType getItemType() {
        if(!initItemType) {
            initItemType = true;
            itemType = ItemTypes.get(getId());
        }
        return itemType;
    }

    /**
     * Get the material for this BlockType.
     *
     * @return The material
     */
    public BlockMaterial getMaterial() {
    	return this.settings.blockMaterial;
    }

    /**
     * Gets the legacy ID. Needed for legacy reasons.
     *
     * DO NOT USE THIS.
     *
     * @return legacy id or 0, if unknown
     */
    public int getLegacyCombinedId() {
        Integer combinedId = LegacyMapper.getInstance().getLegacyCombined(this);
        return combinedId == null ? 0 : combinedId;
    }

    /**
     * The internal index of this type.
     *
     * This number is not necessarily consistent across restarts.
     *
     * @return internal id
     */
    public int getInternalId() {
    	return this.settings.internalId;
    }

    @Override
    public int hashCode() {
        return settings.internalId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
    
    @Override
    public String toString() {
        return getId();
    }
    

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBlock(set, this.getDefaultState());
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return this.getDefaultState().toBaseBlock();
    }

    public Mask toMask(Extent extent) {
        return new SingleBlockTypeMask(extent, this);
    }


    @Deprecated
    public int getLegacyId() { //
        Integer id = LegacyMapper.getInstance().getLegacyCombined(this.getDefaultState());
        if (id != null) {
            return id >> 4;
        } else {
            return 0;
        }
    }
}
