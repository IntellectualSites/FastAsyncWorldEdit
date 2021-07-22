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

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BundledItemRegistry;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;

class BukkitItemRegistry extends BundledItemRegistry {
    @Override
    public Component getRichName(ItemType itemType) {
        if (WorldEditPlugin.getInstance().getBukkitImplAdapter() != null) {
            return WorldEditPlugin.getInstance().getBukkitImplAdapter().getRichItemName(itemType);
        }
        return super.getRichName(itemType);
    }

    @Override
    public Component getRichName(BaseItemStack itemStack) {
        if (WorldEditPlugin.getInstance().getBukkitImplAdapter() != null) {
            return WorldEditPlugin.getInstance().getBukkitImplAdapter().getRichItemName(itemStack);
        }
        return super.getRichName(itemStack);
    }

    //FAWE start
    @Override
    public Collection<String> values() {
        ArrayList<String> values = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy() && (m.isBlock() || m.isItem())) {
                String id = m.getKey().toString();
                values.add(id);
            }
        }
        return values;
    }
    //FAWE end
}
