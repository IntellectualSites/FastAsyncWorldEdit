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

import com.fastasyncworldedit.bukkit.adapter.IBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.SimpleBukkitAdapter;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.item.ItemType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts between Bukkit and WorldEdit equivalent objects.
 */
//FAWE start - enum-ized
public enum BukkitAdapter {
    INSTANCE;

    private final IBukkitAdapter adapter;

    BukkitAdapter() {
        BukkitImplAdapter tmp = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        this.adapter = Objects.requireNonNullElseGet(tmp, SimpleBukkitAdapter::new);
    }

    private static IBukkitAdapter getAdapter() {
        return INSTANCE.adapter;
    }

    //FAWE end

    private static final ParserContext TO_BLOCK_CONTEXT = new ParserContext();

    static {
        TO_BLOCK_CONTEXT.setRestricted(false);
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material.
     *
     * @param blockType The WorldEdit BlockType
     * @param type      The Bukkit Material
     * @return If they are equal
     */
    public static boolean equals(BlockType blockType, Material type) {
        //FAWE start - swapped reference to getAdapter
        return getAdapter().equals(blockType, type);
        //FAWE end
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
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().asBukkitWorld(world);
        //FAWE end
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    public static World adapt(org.bukkit.World world) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(world);
        //FAWE end
    }

    /**
     * Create a WorldEdit Actor from a Bukkit CommandSender.
     *
     * @param sender The Bukkit CommandSender
     * @return The WorldEdit Actor
     */
    public static Actor adapt(CommandSender sender) {
        return WorldEditPlugin.getInstance().wrapCommandSender(sender);
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
     * Create a Bukkit CommandSender from a WorldEdit Actor.
     *
     * @param actor The WorldEdit actor
     * @return The Bukkit command sender
     */
    public static CommandSender adapt(Actor actor) {
        if (actor instanceof com.sk89q.worldedit.entity.Player) {
            return adapt((com.sk89q.worldedit.entity.Player) actor);
        } else if (actor instanceof BukkitBlockCommandSender) {
            return ((BukkitBlockCommandSender) actor).getSender();
        }
        return ((BukkitCommandSender) actor).getSender();
    }

    /**
     * Create a Bukkit Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The Bukkit player
     */
    public static Player adapt(com.sk89q.worldedit.entity.Player player) {
        //FAWE start - Get player from PlayerProxy instead of BukkitPlayer if null
        player = PlayerProxy.unwrap(player);
        return player == null ? null : ((BukkitPlayer) player).getPlayer();
        //FAWE end
    }

    /**
     * Create a WorldEdit Direction from a Bukkit BlockFace.
     *
     * @param face the Bukkit BlockFace
     * @return a WorldEdit direction
     */
    public static Direction adapt(@Nullable BlockFace face) {
        if (face == null) {
            return null;
        }
        switch (face) {
            case NORTH:
                return Direction.NORTH;
            case SOUTH:
                return Direction.SOUTH;
            case WEST:
                return Direction.WEST;
            case EAST:
                return Direction.EAST;
            case DOWN:
                return Direction.DOWN;
            case UP:
            default:
                return Direction.UP;
        }
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    public static org.bukkit.World adapt(World world) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(world);
        //FAWE end
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
                location.getPitch()
        );
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
                position.x(), position.y(), position.z(),
                location.getYaw(),
                location.getPitch()
        );
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world    the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Vector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.x(), position.y(), position.z()
        );
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world    the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, BlockVector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.x(), position.y(), position.z()
        );
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param world    the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Location location) {
        checkNotNull(world);
        checkNotNull(location);
        return new org.bukkit.Location(
                world,
                location.x(), location.y(), location.z(),
                location.getYaw(),
                location.getPitch()
        );
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
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(entity);
        //FAWE end
    }

    /**
     * Create a Bukkit Material form a WorldEdit ItemType.
     *
     * @param itemType The WorldEdit ItemType
     * @return The Bukkit Material
     */
    public static Material adapt(ItemType itemType) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(itemType);
        //FAWE end
    }

    /**
     * Create a Bukkit Material form a WorldEdit BlockType.
     *
     * @param blockType The WorldEdit BlockType
     * @return The Bukkit Material
     */
    public static Material adapt(BlockType blockType) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(blockType);
        //FAWE end
    }

    /**
     * Create a WorldEdit GameMode from a Bukkit one.
     *
     * @param gameMode Bukkit GameMode
     * @return WorldEdit GameMode
     */
    public static GameMode adapt(org.bukkit.GameMode gameMode) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(gameMode);
        //FAWE end
    }

    /**
     * Create a WorldEdit BiomeType from a Bukkit one.
     *
     * @param biome Bukkit Biome
     * @return WorldEdit BiomeType
     */
    public static BiomeType adapt(Biome biome) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(biome);
        //FAWE end
    }

    public static Biome adapt(BiomeType biomeType) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(biomeType);
        //FAWE end
    }

    /**
     * Create a WorldEdit EntityType from a Bukkit one.
     *
     * @param entityType Bukkit EntityType
     * @return WorldEdit EntityType
     */
    public static EntityType adapt(org.bukkit.entity.EntityType entityType) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(entityType);
        //FAWE end
    }

    public static org.bukkit.entity.EntityType adapt(EntityType entityType) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(entityType);
        //FAWE end
    }

    private static final EnumMap<Material, BlockType> materialBlockTypeCache = new EnumMap<>(Material.class);
    private static final EnumMap<Material, ItemType> materialItemTypeCache = new EnumMap<>(Material.class);

    /**
     * Converts a Material to a BlockType.
     *
     * @param material The material
     * @return The blocktype
     */
    @Nullable
    public static BlockType asBlockType(Material material) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().asBlockType(material);
        //FAWE end
    }

    /**
     * Converts a Material to a ItemType.
     *
     * @param material The material
     * @return The itemtype
     */
    @Nullable
    public static ItemType asItemType(Material material) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().asItemType(material);
        //FAWE end
    }

    private static final Int2ObjectMap<BlockState> blockStateCache = new Int2ObjectOpenHashMap<>();
    private static final Map<String, BlockState> blockStateStringCache = new HashMap<>();

    /**
     * Create a WorldEdit BlockState from a Bukkit BlockData.
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    public static BlockState adapt(@Nonnull BlockData blockData) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(blockData);
        //FAWE end
    }

    private static final Int2ObjectMap<BlockData> blockDataCache = new Int2ObjectOpenHashMap<>();

    /**
     * Create a Bukkit BlockData from a WorldEdit BlockStateHolder.
     *
     * @param block The WorldEdit BlockStateHolder
     * @return The Bukkit BlockData
     */
    public static BlockData adapt(@Nonnull BlockStateHolder block) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(block);
        //FAWE end
    }

    /**
     * Create a WorldEdit BlockState from a Bukkit ItemStack.
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BlockState
     */
    public static BlockState asBlockState(ItemStack itemStack) throws WorldEditException {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().asBlockState(itemStack);
        //FAWE end
    }

    /**
     * Create a WorldEdit BaseItemStack from a Bukkit ItemStack.
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BaseItemStack
     */
    public static BaseItemStack adapt(ItemStack itemStack) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(itemStack);
        //FAWE end
    }

    /**
     * Create a Bukkit ItemStack from a WorldEdit BaseItemStack.
     *
     * @param item The WorldEdit BaseItemStack
     * @return The Bukkit ItemStack
     */
    public static ItemStack adapt(BaseItemStack item) {
        //FAWE start - logic moved to IBukkitAdapter
        return getAdapter().adapt(item);
        //FAWE end
    }
}
