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

package com.sk89q.worldedit.world.registry;

<<<<<<< HEAD
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
=======
import com.sk89q.worldedit.world.item.ItemType;

import javax.annotation.Nullable;
>>>>>>> b75d5149... Fixed the bundle being directly used outside of the registry system.

/**
 * A item registry that uses {@link BundledItemRegistry} to serve information
 * about items.
 */
public class BundledItemRegistry implements ItemRegistry {

    @Nullable
    @Override
<<<<<<< HEAD
    public BaseItem createFromId(String id) {
        ItemType itemType = ItemTypes.get(id);
        return itemType == null ? null : new BaseItem(itemType);
    }

    @Override
    public Collection<String> registerItems() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
=======
>>>>>>> b75d5149... Fixed the bundle being directly used outside of the registry system.
    public String getName(ItemType itemType) {
        BundledItemData.ItemEntry itemEntry = BundledItemData.getInstance().findById(itemType.getId());
        return itemEntry != null ? itemEntry.localizedName : null;
    }
}
