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

package com.sk89q.worldedit.bukkit;

import com.bekvon.bukkit.residence.commands.material;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.command.tool.BlockDataCyler;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.sk89q.worldedit.world.registry.PassthroughBlockMaterial;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

public class BukkitBlockRegistry extends BundledBlockRegistry {

    private BukkitBlockMaterial[] materialMap;

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(blockType);
            if (result != null) return result;
        }
        Material type = BukkitAdapter.adapt(blockType);
        if (type == null) {
            if (blockType == BlockTypes.__RESERVED__) return new PassthroughBlockMaterial(super.getMaterial(BlockTypes.AIR));
            return new PassthroughBlockMaterial(null);
        }
        if (materialMap == null) {
            materialMap = new BukkitBlockMaterial[Material.values().length];
        }
        BukkitBlockMaterial result = materialMap[type.ordinal()];
        if (result == null) {
            result = new BukkitBlockMaterial(BukkitBlockRegistry.super.getMaterial(blockType), type);
            materialMap[type.ordinal()] = result;
        }
        return result;
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockState state) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(state);
            if (result != null) return result;
        }
        return super.getMaterial(state);
    }

    @Nullable
    @Override
    public Map<String, ? extends Property> getProperties(BlockType blockType) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.getProperties(blockType);
        }
        return super.getProperties(blockType);
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

        @Override
        public boolean isTranslucent() {
            return material.isTransparent();
        }
    }

    @Override
    public Collection<String> registerBlocks() {
        ArrayList<String> blocks = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.isBlock()) {
                BlockData blockData = m.createBlockData();
                blocks.add(blockData.getAsString());
            }
        }
        return blocks;
    }
}
