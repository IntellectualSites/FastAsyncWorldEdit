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
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.IBukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.SimpleBukkitAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.item.ItemType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts between Bukkit and WorldEdit equivalent objects.
 */
public enum BukkitAdapter {
    INSTANCE
    ;
    private final IBukkitAdapter adapter;

    BukkitAdapter() {
        BukkitImplAdapter tmp = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (tmp != null) {
            this.adapter = tmp;
        } else {
            this.adapter = new SimpleBukkitAdapter();
        }
    }

    private static final IBukkitAdapter getAdapter() {
        return INSTANCE.adapter;
    }

    public static boolean equals(BlockType blockType, Material type) {
        return getAdapter().equals(blockType, type);
    }

    public static BukkitWorld asBukkitWorld(World world) {
        return getAdapter().asBukkitWorld(world);
    }

    public static World adapt(org.bukkit.World world) {
        return getAdapter().adapt(world);
    }

    public static org.bukkit.World adapt(World world) {
        return getAdapter().adapt(world);
    }

    public static Location adapt(org.bukkit.Location location) {
        return getAdapter().adapt(location);
    }

    public static org.bukkit.Location adapt(Location location) {
        return getAdapter().adapt(location);
    }

    public static org.bukkit.Location adapt(org.bukkit.World world, Vector position) {
        return getAdapter().adapt(world, position);
    }

    public static org.bukkit.Location adapt(org.bukkit.World world, Location location) {
        return getAdapter().adapt(world, location);
    }

    public static Vector asVector(org.bukkit.Location location) {
        return getAdapter().asVector(location);
    }

    public static Entity adapt(org.bukkit.entity.Entity entity) {
        return getAdapter().adapt(entity);
    }

    public static Material adapt(ItemType itemType) {
        return getAdapter().adapt(itemType);
    }

    public static Material adapt(BlockType blockType) {
        return getAdapter().adapt(blockType);
    }

    public static GameMode adapt(org.bukkit.GameMode gameMode) {
        return getAdapter().adapt(gameMode);
    }

    public static EntityType adapt(org.bukkit.entity.EntityType entityType) {
        return getAdapter().adapt(entityType);
    }

    public static org.bukkit.entity.EntityType adapt(EntityType entityType) {
        return getAdapter().adapt(entityType);
    }

    public static BlockType asBlockType(Material material) {
        return getAdapter().asBlockType(material);
    }

    public static ItemType asItemType(Material material) {
        return getAdapter().asItemType(material);
    }

    public static BlockState adapt(BlockData blockData) {
        return getAdapter().adapt(blockData);
    }

    public static BlockTypes adapt(Material material) {
        return getAdapter().adapt(material);
    }

    public static BlockData adapt(BlockStateHolder block) {
        return getAdapter().adapt(block);
    }

    public static BlockData getBlockData(int combinedId) {
        return getAdapter().getBlockData(combinedId);
    }

    public static BlockState asBlockState(ItemStack itemStack) {
        return getAdapter().asBlockState(itemStack);
    }

    public static BaseItemStack adapt(ItemStack itemStack) {
        return getAdapter().adapt(itemStack);
    }

    public static ItemStack adapt(BaseItemStack item) {
        return getAdapter().adapt(item);
    }

    public static BukkitPlayer adapt(Player player) {
        return getAdapter().adapt(player);
    }

    public static Player adapt(com.sk89q.worldedit.entity.Player player) {
        return getAdapter().adapt(player);
    }
}
