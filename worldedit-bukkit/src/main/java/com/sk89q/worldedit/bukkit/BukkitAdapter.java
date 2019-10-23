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

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.IBukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.SimpleBukkitAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts between Bukkit and WorldEdit equivalent objects.
 */
public enum BukkitAdapter {
    INSTANCE;

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

    private static final ParserContext TO_BLOCK_CONTEXT = new ParserContext();

    static {
        TO_BLOCK_CONTEXT.setRestricted(false);
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material
     *
     * @param blockType The WorldEdit BlockType
     * @param type The Bukkit Material
     * @return If they are equal
     */
    public static boolean equals(BlockType blockType, Material type) {
        return getAdapter().equals(blockType, type);
    }

    /**
     * Convert any WorldEdit world into an equivalent wrapped Bukkit world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped Bukkit world
     */
    public static BukkitWorld asBukkitWorld(World world) {
        return getAdapter().asBukkitWorld(world);
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    public static World adapt(org.bukkit.World world) {
        return getAdapter().adapt(world);
    }

    /**
     * Create a WorldEdit Player from a Bukkit Player.
     *
     * @param player The Bukkit player
     * @return The WorldEdit player
     */
    public static BukkitPlayer adapt(Player player) {
        return WorldEditPlugin.getInstance().wrapPlayer(player);
    }

    /**
     * Create a Bukkit Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The Bukkit player
     */
    public static Player adapt(com.sk89q.worldedit.entity.Player player) {
        player = PlayerProxy.unwrap(player);
        return ((BukkitPlayer) player).getPlayer();
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    public static org.bukkit.World adapt(World world) {
        return getAdapter().adapt(world);
    }

    /**
     * Create a WorldEdit location from a Bukkit location.
     *
     * @param location the Bukkit location
     * @return a WorldEdit location
     */
    public static Location adapt(org.bukkit.Location location) {
        checkNotNull(location);
        Vector3 position = asVector(location);
        return new com.sk89q.worldedit.util.Location(
                adapt(location.getWorld()),
                position,
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit location.
     *
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(Location location) {
        checkNotNull(location);
        Vector3 position = location;
        return new org.bukkit.Location(
                adapt((World) location.getExtent()),
                position.getX(), position.getY(), position.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Vector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.getX(), position.getY(), position.getZ());
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, BlockVector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.getX(), position.getY(), position.getZ());
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Location location) {
        checkNotNull(world);
        checkNotNull(location);
        return new org.bukkit.Location(
                world,
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a WorldEdit Vector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    public static Vector3 asVector(org.bukkit.Location location) {
        checkNotNull(location);
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit BlockVector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    public static BlockVector3 asBlockVector(org.bukkit.Location location) {
        checkNotNull(location);
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit entity from a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return a WorldEdit entity
     */
    public static Entity adapt(org.bukkit.entity.Entity entity) {
        return getAdapter().adapt(entity);
    }

    /**
     * Create a Bukkit Material form a WorldEdit ItemType
     *
     * @param itemType The WorldEdit ItemType
     * @return The Bukkit Material
     */
    public static Material adapt(ItemType itemType) {
        return getAdapter().adapt(itemType);
    }

    /**
     * Create a Bukkit Material form a WorldEdit BlockType
     *
     * @param blockType The WorldEdit BlockType
     * @return The Bukkit Material
     */
    public static Material adapt(BlockType blockType) {
        return getAdapter().adapt(blockType);
    }

    /**
     * Create a WorldEdit GameMode from a Bukkit one.
     *
     * @param gameMode Bukkit GameMode
     * @return WorldEdit GameMode
     */
    public static GameMode adapt(org.bukkit.GameMode gameMode) {
        return getAdapter().adapt(gameMode);
    }

    /**
     * Create a WorldEdit BiomeType from a Bukkit one.
     *
     * @param biome Bukkit Biome
     * @return WorldEdit BiomeType
     */
    public static BiomeType adapt(Biome biome) {
        return getAdapter().adapt(biome);
    }

    public static Biome adapt(BiomeType biomeType) {
        return getAdapter().adapt(biomeType);
    }

    /**
     * Create a WorldEdit EntityType from a Bukkit one.
     *
     * @param entityType Bukkit EntityType
     * @return WorldEdit EntityType
     */
    public static EntityType adapt(org.bukkit.entity.EntityType entityType) {
        return getAdapter().adapt(entityType);
    }

    public static org.bukkit.entity.EntityType adapt(EntityType entityType) {
        return getAdapter().adapt(entityType);
    }

    /**
     * Converts a Material to a BlockType
     *
     * @param material The material
     * @return The blocktype
     */
    public static BlockType asBlockType(Material material) {
        return getAdapter().asBlockType(material);
    }

    /**
     * Converts a Material to a ItemType
     *
     * @param material The material
     * @return The itemtype
     */
    public static ItemType asItemType(Material material) {
        return getAdapter().asItemType(material);
    }
    /*
    private static Map<String, BlockState> blockStateCache = new HashMap<>();
    /*

    /**
     * Create a WorldEdit BlockState from a Bukkit BlockData
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    public static BlockState adapt(@NotNull BlockData blockData) {
        return getAdapter().adapt(blockData);
    }

    public static BlockType adapt(Material material) {
        return getAdapter().adapt(material);
    }
    /*
    private static Map<String, BlockData> blockDataCache = new HashMap<>();
    */
    /**
     * Create a Bukkit BlockData from a WorldEdit BlockStateHolder
     *
     * @param block The WorldEdit BlockStateHolder
     * @return The Bukkit BlockData
     */
    public static BlockData adapt(@NotNull BlockStateHolder block) {
        return getAdapter().adapt(block);
    }

    /**
     * Create a WorldEdit BlockState from a Bukkit ItemStack
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BlockState
     */
    public static BlockState asBlockState(ItemStack itemStack) throws WorldEditException {
        return getAdapter().asBlockState(itemStack);
    }

    /**
     * Create a WorldEdit BaseItemStack from a Bukkit ItemStack
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BaseItemStack
     */
    public static BaseItemStack adapt(ItemStack itemStack) {
        return getAdapter().adapt(itemStack);
    }

    /**
     * Create a Bukkit ItemStack from a WorldEdit BaseItemStack
     *
     * @param item The WorldEdit BaseItemStack
     * @return The Bukkit ItemStack
     */
    public static ItemStack adapt(BaseItemStack item) {
        return getAdapter().adapt(item);
    }
}
