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

import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;
import com.sk89q.worldedit.world.registry.PassthroughBlockMaterial;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class BukkitBlockRegistry extends BundledBlockRegistry {

    private BukkitBlockMaterial[] materialMap;

    @Override
    public Component getRichName(BlockType blockType) {
        if (WorldEditPlugin.getInstance().getBukkitImplAdapter() != null) {
            return WorldEditPlugin.getInstance().getBukkitImplAdapter().getRichBlockName(blockType);
        }
        return super.getRichName(blockType);
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        //FAWE start - delegate to our internal values
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(blockType);
            if (result != null) {
                return result;
            }
        }
        Material mat = BukkitAdapter.adapt(blockType);
        if (mat == null) {
            return new PassthroughBlockMaterial(null);
        }
        if (materialMap == null) {
            materialMap = new BukkitBlockMaterial[Material.values().length];
        }
        BukkitBlockMaterial result = materialMap[mat.ordinal()];
        if (result == null) {
            result = new BukkitBlockMaterial(BukkitBlockRegistry.super.getMaterial(blockType), mat);
            materialMap[mat.ordinal()] = result;
        }
        return result;
        //FAWE end
    }

    //FAWE start
    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockState state) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(state);
            if (result != null) {
                return result;
            }
        }
        return super.getMaterial(state);
    }
    //FAWE end

    @Nullable
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.getProperties(blockType);
        }
        return super.getProperties(blockType);
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        if (WorldEditPlugin.getInstance().getBukkitImplAdapter() != null) {
            return WorldEditPlugin.getInstance().getBukkitImplAdapter().getInternalBlockStateId(state);
        }
        return OptionalInt.empty();
    }

    public static class BukkitBlockMaterial extends PassthroughBlockMaterial {

        private final Material material;

        public BukkitBlockMaterial(@Nullable BlockMaterial material, Material bukkitMaterial) {
            super(material);
            this.material = bukkitMaterial;
        }

        @Override
        public boolean isAir() {
            switch (material) {
                case AIR:
                case CAVE_AIR:
                case VOID_AIR:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean isSolid() {
            return material.isSolid();
        }

        @Override
        public boolean isBurnable() {
            return material.isBurnable();
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean isTranslucent() {
            return material.isTransparent();
        }

    }

    //FAWE start
    @Override
    public Collection<String> values() {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.getRegisteredDefaultBlockStates();
        }
        return super.values();
    }

    @Override
    public Map<String, ? extends List<Property<?>>> getAllProperties() {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.getAllProperties();
        }
        return super.getAllProperties();
    }
    //FAWE end
}
