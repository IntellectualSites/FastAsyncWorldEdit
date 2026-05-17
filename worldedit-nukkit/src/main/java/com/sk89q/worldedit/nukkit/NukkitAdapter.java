package com.sk89q.worldedit.nukkit;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static adapter utility class for converting between Nukkit and WorldEdit types.
 */
public final class NukkitAdapter {

    private static final Map<Level, NukkitWorld> worldCache = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, NukkitPlayer> playerCache = Collections.synchronizedMap(new WeakHashMap<>());

    private NukkitAdapter() {
    }

    public static NukkitWorld adapt(Level level) {
        return worldCache.computeIfAbsent(level, NukkitWorld::new);
    }

    public static NukkitPlayer adapt(Player player) {
        return playerCache.computeIfAbsent(player, NukkitPlayer::new);
    }

    /**
     * Remove a player from the cache. Should be called on player quit.
     */
    static void uncachePlayer(Player player) {
        playerCache.remove(player);
    }

    /**
     * Convert a WorldEdit World/Extent to a Nukkit Level.
     */
    public static Level adapt(Extent world) {
        checkNotNull(world);
        if (world instanceof NukkitWorld nukkitWorld) {
            return nukkitWorld.getLevel();
        }
        throw new IllegalArgumentException("Extent is not a NukkitWorld: " + world.getClass().getName());
    }

    public static Vector3 adapt(BlockVector3 position) {
        return new Vector3(position.x(), position.y(), position.z());
    }

    public static Vector3 adapt(com.sk89q.worldedit.math.Vector3 position) {
        return new Vector3(position.x(), position.y(), position.z());
    }

    public static BlockVector3 adapt(Vector3 position) {
        return BlockVector3.at(position.getFloorX(), position.getFloorY(), position.getFloorZ());
    }

    /**
     * Convert a WorldEdit Location to a Nukkit Location.
     */
    public static cn.nukkit.level.Location adapt(Location location) {
        checkNotNull(location);
        return new cn.nukkit.level.Location(
                location.x(), location.y(), location.z(),
                location.getYaw(), location.getPitch(),
                adapt(location.getExtent())
        );
    }

    /**
     * Convert a Nukkit Location to a WorldEdit Location.
     */
    public static Location adapt(cn.nukkit.level.Location location) {
        checkNotNull(location);
        return new Location(
                adapt(location.getLevel()),
                location.x, location.y, location.z,
                (float) location.yaw, (float) location.pitch
        );
    }

    /**
     * Convert a Nukkit Item to a WorldEdit ItemType.
     */
    public static ItemType adapt(Item item) {
        String jeId = ItemMapping.beToJe(item.getId(), item.getDamage());
        ItemType type = ItemTypes.get(jeId);
        return type != null ? type : ItemTypes.AIR;
    }

    /**
     * Convert a Nukkit Item to a WorldEdit BaseItemStack (with count).
     */
    public static BaseItemStack adaptItemStack(Item item) {
        return new BaseItemStack(adapt(item), item.getCount());
    }

    /**
     * Convert a WorldEdit ItemType to a Nukkit Item.
     */
    public static Item adapt(ItemType itemType) {
        ItemMapping.NukkitItemData data = ItemMapping.jeToBe(itemType.id());
        return Item.get(data.itemId(), data.metadata());
    }

    /**
     * Convert a WorldEdit BaseItemStack to a Nukkit Item (with count).
     */
    public static Item adaptItem(BaseItemStack itemStack) {
        ItemMapping.NukkitItemData data = ItemMapping.jeToBe(itemStack.getType().id());
        return Item.get(data.itemId(), data.metadata(), itemStack.getAmount());
    }

    /**
     * Convert a Nukkit block fullId to a WorldEdit BlockState.
     */
    public static BlockState adaptBlockState(int fullId) {
        char ordinal = BlockMapping.fullIdToJeOrdinal(fullId);
        if (ordinal == Character.MAX_VALUE) {
            return BlockTypes.AIR.getDefaultState();
        }
        BlockState state = com.sk89q.worldedit.world.block.BlockTypesCache.states[ordinal];
        return state != null ? state : BlockTypes.AIR.getDefaultState();
    }

    /**
     * Convert a WorldEdit BlockState to a Nukkit fullId.
     */
    public static int adaptFullId(BlockState state) {
        return BlockMapping.jeOrdinalToFullId(state.getOrdinalChar());
    }

}
