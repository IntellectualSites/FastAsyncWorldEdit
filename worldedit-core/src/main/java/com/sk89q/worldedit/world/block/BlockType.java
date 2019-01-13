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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.registry.NamespacedRegistry;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.util.*;

public interface BlockType extends FawePattern, Comparable<BlockTypes> {

    default BlockTypes getTypeEnum() {
        return (BlockTypes) this;
    }

    @Deprecated
    int getMaxStateId();

    @Override
    default boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBlock(set, this.getDefaultState());
    }

    @Override
    default BlockStateHolder apply(BlockVector3 position) {
        return this.getDefaultState();
    }

    default Mask toMask(Extent extent) {
        return new SingleBlockTypeMask(extent, this);
    }

    /**
     * Gets the ID of this block.
     *
     * @return The id
     */
    String getId();

    default String getNamespace() {
        String id = getId();
        int i = id.indexOf(':');
        return i == -1 ? "minecraft" : id.substring(0, i);
    }

    default String getResource() {
        String id = getId();
        return id.substring(id.indexOf(':') + 1);
    }

    /**
     * Gets the name of this block, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    default String getName() {
        BundledBlockData.BlockEntry entry = BundledBlockData.getInstance().findById(this.getId());
        if (entry == null) {
            return getId();
        } else {
            return entry.localizedName;
        }
    }

    @Deprecated
    default BlockState withPropertyId(int internalPropertiesId) {
        if (internalPropertiesId == 0) return getDefaultState();
        return BlockState.getFromInternalId(getInternalId() + (internalPropertiesId << BlockTypes.BIT_OFFSET));
    }

    /**
     * Gets the properties of this BlockType in a {@code key->property} mapping.
     *
     * @return The properties map
     */
    @Deprecated
    default Map<String, ? extends Property> getPropertyMap() {
        List<? extends Property> properties = getProperties();
        if (properties.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Property> map = new HashMap<>(properties.size());
        for (Property property : properties) {
            map.put(property.getName(), property);
        }
        return map;
    }

    /**
     * Gets the properties of this BlockType.
     *
     * @return the properties
     */
    @Deprecated
    List<? extends Property> getProperties();

    @Deprecated
    default Set<? extends Property> getPropertiesSet() {
        return new HashSet<>(getProperties());
    }

    /**
     * Gets a property by name.
     *
     * @param name The name
     * @return The property
     */
    @Deprecated
    default <V> Property<V> getProperty(String name) {
        Property<V> property = getPropertyMap().get(name);
        checkArgument(property != null, "%s has no property named %s", this, name);
        return property;
    }

    default boolean hasProperty(PropertyKey key) {
        return getPropertyMap().containsKey(key.getId());
    }

    default <V> Property<V> getProperty(PropertyKey key) {
        Property<V> property = getPropertyMap().get(key.getId());
        checkArgument(property != null, "%s has no property named %s", this, key.getId());
        return property;
    }

    /**
     * Gets the default state of this block type.
     *
     * @return The default state
     */
    BlockState getDefaultState();

    /**
     * Gets a list of all possible states for this BlockType.
     *
     * @return All possible states
     */
    List<BlockState> getAllStates();

    /**
     * Gets a state of this BlockType with the given properties.
     *
     * @return The state, if it exists
     */
    BlockState getState(Map<Property<?>, Object> key);

    /**
     * Gets whether this block type has an item representation.
     *
     * @return If it has an item
     */
    default boolean hasItemType() {
        return getItemType() != null;
    }

    /**
     * Gets the item representation of this block type, if it exists.
     *
     * @return The item representation
     */
    @Nullable
    default ItemType getItemType() {
        return ItemTypes.get(this.getTypeEnum());
    }

    /**
     * Get the material for this BlockType.
     *
     * @return The material
     */
    BlockMaterial getMaterial();

    /**
     * Gets the legacy ID. Needed for legacy reasons.
     *
     * DO NOT USE THIS.
     *
     * @return legacy id or 0, if unknown
     */
    default int getLegacyCombinedId() {
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
    int getInternalId();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    @Deprecated
    default int getLegacyId() {
        Integer id = LegacyMapper.getInstance().getLegacyCombined(this.getDefaultState());
        if (id != null) {
            return id >> 4;
        } else {
            return 0;
        }
    }
}
