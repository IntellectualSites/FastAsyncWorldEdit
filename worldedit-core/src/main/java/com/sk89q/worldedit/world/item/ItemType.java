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

package com.sk89q.worldedit.world.item;

import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;

public interface ItemType {

    default ItemTypes toEnum() {
        return (ItemTypes) this;
    }

    String getId();

    int getInternalId();

    /**
     * Gets the name of this item, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    String getName();


    /**
     * Gets whether this item type has a block representation.
     *
     * @return If it has a block
     */
    default boolean hasBlockType() {
        return getBlockType() != null;
    }

    /**
     * Gets the block representation of this item type, if it exists.
     *
     * @return The block representation
     */
    @Nullable
    default BlockTypes getBlockType() {
        return BlockTypes.get(getId());
    }

    default BaseItem getDefaultState() {
        return new BaseItem(this);
    }
}
