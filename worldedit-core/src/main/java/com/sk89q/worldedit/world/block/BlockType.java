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

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.pattern.FawePattern;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlockType extends FawePattern, Comparable<BlockTypes> {

    default BlockTypes getTypeEnum() {
        return (BlockTypes) this;
    }

    @Deprecated
    int getMaxStateId();

    @Override
    default boolean apply(Extent extent, Vector get, Vector set) throws WorldEditException {
        return extent.setBlock(set, this.getDefaultState());
    }

    @Override
    default BlockStateHolder apply(Vector position) {
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
        return BlockState.get(getInternalId() + (internalPropertiesId << BlockTypes.BIT_OFFSET));
    }

    /**
     * Gets the properties of this BlockType in a key->property mapping.
     *
     * @return The properties map
     */
    @Deprecated
    Map<String, ? extends Property> getPropertyMap();

    /**
     * Gets the properties of this BlockType.
     *
     * @return the properties
     */
    @Deprecated
    List<? extends Property> getProperties();

    @Deprecated
    Set<? extends Property> getPropertiesSet();

    /**
     * Gets a property by name.
     *
     * @param name The name
     * @return The property
     */
    @Deprecated
    <V> Property<V> getProperty(String name);

    boolean hasProperty(PropertyKey key);

    <V> Property<V> getProperty(PropertyKey key);

    /**
     * Gets the default state of this block type.
     *
     * @return The default state
     */
    BlockState getDefaultState();

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
    ItemType getItemType();

    /**
     * Get the material for this BlockType.
     *
     * @return The material
     */
    BlockMaterial getMaterial();

    default int getLegacyCombinedId() {
        Integer combinedId = LegacyMapper.getInstance().getLegacyFromBlock(this);
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
}
