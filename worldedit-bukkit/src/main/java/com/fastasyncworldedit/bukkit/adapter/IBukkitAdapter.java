package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.bukkit.util.BukkitItemStack;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitEntity;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.EditSessionBlockChangeDelegate;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.TreeType;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public interface IBukkitAdapter {

    /**
     * Convert any WorldEdit world into an equivalent wrapped Bukkit world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped Bukkit world
     */
    default BukkitWorld asBukkitWorld(World world) {
        if (world instanceof BukkitWorld) {
            return (BukkitWorld) world;
        } else {
            BukkitWorld bukkitWorld = WorldEditPlugin.getInstance().getInternalPlatform().matchWorld(world);
            if (bukkitWorld == null) {
                throw new RuntimeException("World '" + world.getName() + "' has no matching version in Bukkit");
            }
            return bukkitWorld;
        }
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    default org.bukkit.World adapt(World world) {
        checkNotNull(world);
        if (world instanceof BukkitWorld) {
            return ((BukkitWorld) world).getWorld();
        } else {
            org.bukkit.World match = Bukkit.getServer().getWorld(world.getName());
            if (match != null) {
                return match;
            } else {
                throw new IllegalArgumentException("Can't find a Bukkit world for " + world);
            }
        }
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world    the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    default org.bukkit.Location adapt(org.bukkit.World world, Vector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.x(), position.y(), position.z()
        );
    }

    default org.bukkit.Location adapt(org.bukkit.World world, BlockVector3 position) {
        return adapt(world, position.toVector3());
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param world    the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    default org.bukkit.Location adapt(org.bukkit.World world, Location location) {
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
    default Vector3 asVector(org.bukkit.Location location) {
        checkNotNull(location);
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit BlockVector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    default BlockVector3 asBlockVector(org.bukkit.Location location) {
        checkNotNull(location);
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit entity from a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return a WorldEdit entity
     */
    default Entity adapt(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);
        return new BukkitEntity(entity);
    }

    /**
     * Create a Bukkit Material form a WorldEdit ItemType
     *
     * @param itemType The WorldEdit ItemType
     * @return The Bukkit Material
     */
    default Material adapt(ItemType itemType) {
        checkNotNull(itemType);
        if (!itemType.id().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Bukkit only supports Minecraft items");
        }
        return Material.getMaterial(itemType.id().substring(10).toUpperCase(Locale.ROOT));
    }

    /**
     * Create a Bukkit Material form a WorldEdit BlockType
     *
     * @param blockType The WorldEdit BlockType
     * @return The Bukkit Material
     */
    default Material adapt(BlockType blockType) {
        checkNotNull(blockType);
        if (!blockType.id().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Bukkit only supports Minecraft blocks");
        }
        String id = blockType.id().substring(10).toUpperCase(Locale.ROOT);
        return Material.getMaterial(id);
    }

    default org.bukkit.entity.EntityType adapt(EntityType entityType) {
        NamespacedKey entityKey = NamespacedKey.fromString(entityType.toString());
        if (entityKey == null) {
            throw new IllegalArgumentException("Entity key '" + entityType + "' does not map to Bukkit");
        }

        return Registry.ENTITY_TYPE.get(entityKey);
    }

    /**
     * Converts a Material to a BlockType
     *
     * @param material The material
     * @return The blocktype
     */
    default BlockType asBlockType(Material material) {
        checkNotNull(material);
        if (!material.isBlock()) {
            throw new IllegalArgumentException(material.getKey() + " is not a block!") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };
        }
        return BlockTypes.get(material.getKey().toString());
    }


    /**
     * Converts a Material to a ItemType
     *
     * @param material The material
     * @return The itemtype
     */
    default ItemType asItemType(Material material) {
        return ItemTypes.get(material.getKey().toString());
    }

    /**
     * Create a WorldEdit BlockStateHolder from a Bukkit BlockData
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    default BlockState adapt(BlockData blockData) {
        String id = blockData.getAsString();
        return BlockState.get(id);
    }

    /**
     * Create a Bukkit BlockData from a WorldEdit BlockStateHolder
     *
     * @param block The WorldEdit BlockStateHolder
     * @return The Bukkit BlockData
     */
    default <B extends BlockStateHolder<B>> BlockData adapt(B block) {
        return Bukkit.createBlockData(block.getAsString());
    }

    /**
     * Create a WorldEdit BaseItemStack from a Bukkit ItemStack
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BaseItemStack
     */
    default BaseItemStack adapt(ItemStack itemStack) {
        checkNotNull(itemStack);
        return new BukkitItemStack(itemStack);
    }

    /**
     * Create a Bukkit ItemStack from a WorldEdit BaseItemStack
     *
     * @param item The WorldEdit BaseItemStack
     * @return The Bukkit ItemStack
     */
    default ItemStack adapt(BaseItemStack item) {
        checkNotNull(item);
        if (item instanceof BukkitItemStack) {
            return ((BukkitItemStack) item).getBukkitItemStack();
        }
        return new ItemStack(adapt(item.getType()), item.getAmount());
    }

    /**
     * Create a WorldEdit Player from a Bukkit Player.
     *
     * @param player The Bukkit player
     * @return The WorldEdit player
     */
    default BukkitPlayer adapt(Player player) {
        return WorldEditPlugin.getInstance().wrapPlayer(player);
    }

    /**
     * Create a Bukkit Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The Bukkit player
     */
    default Player adapt(com.sk89q.worldedit.entity.Player player) {
        return ((BukkitPlayer) player).getPlayer();
    }

    default Biome adapt(BiomeType biomeType) {
        if (!biomeType.id().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Bukkit only supports vanilla biomes");
        }
        try {
            return Biome.valueOf(biomeType.id().substring(10).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default BiomeType adapt(Biome biome) {
        return BiomeTypes.get(biome.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material
     *
     * @param blockType The WorldEdit BlockType
     * @param type      The Bukkit Material
     * @return If they are equal
     */
    default boolean equals(BlockType blockType, Material type) {
        return blockType == asItemType(type).getBlockType();
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    default World adapt(org.bukkit.World world) {
        checkNotNull(world);
        return new BukkitWorld(world);
    }

    /**
     * Create a WorldEdit GameMode from a Bukkit one.
     *
     * @param gameMode Bukkit GameMode
     * @return WorldEdit GameMode
     */
    default GameMode adapt(org.bukkit.GameMode gameMode) {
        checkNotNull(gameMode);
        return GameModes.get(gameMode.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Create a WorldEdit EntityType from a Bukkit one.
     *
     * @param entityType Bukkit EntityType
     * @return WorldEdit EntityType
     */
    default EntityType adapt(org.bukkit.entity.EntityType entityType) {
        return EntityTypes.get(entityType.getKey().toString());
    }

    /**
     * Create a WorldEdit BlockStateHolder from a Bukkit ItemStack
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BlockState
     */
    default BlockState asBlockState(ItemStack itemStack) {
        checkNotNull(itemStack);
        if (itemStack.getType().isBlock()) {
            return adapt(itemStack.getType().createBlockData());
        } else {
            throw new NotABlockException();
        }
    }

    /**
     * Generate a given tree type to the given editsession.
     *
     * @param type        Type of tree to generate
     * @param editSession Editsession to set blocks to
     * @param pt          Point to generate tree at
     * @param world       World to "generate" tree from (seed-wise)
     * @return If successsful
     */
    default boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 pt, org.bukkit.World world) {
        TreeType bukkitType = BukkitWorld.toBukkitTreeType(type);
        if (bukkitType == TreeType.CHORUS_PLANT) {
            pt = pt.add(0, 1, 0); // bukkit skips the feature gen which does this offset normally, so we have to add it back
        }
        return type != null && world.generateTree(
                BukkitAdapter.adapt(world, pt), bukkitType,
                new EditSessionBlockChangeDelegate(editSession)
        );
    }

    /**
     * Retrieve the list of Bukkit entities ({@link org.bukkit.entity.Entity}) in the given world. If overridden by adapters
     * will attempt retrieval asynchronously.
     *
     * @param world world to retrieve entities in
     * @return list of {@link org.bukkit.entity.Entity}
     */
    default List<org.bukkit.entity.Entity> getEntities(org.bukkit.World world) {
        return TaskManager.taskManager().sync(world::getEntities);
    }

}
