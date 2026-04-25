package com.fastasyncworldedit.nukkit.adapter;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter interface abstracting API differences between Nukkit-MOT and NKX.
 * <p>
 * Nukkit has two major active forks (MOT and NKX) with incompatible APIs.
 * Unlike Bukkit, where FAWE compiles separate adapter modules per Minecraft
 * version, Nukkit adapters must be detected and loaded at runtime because
 * both forks share the same package names ({@code cn.nukkit}) but have different
 * classes and methods. Compile-time separation is impossible without shading
 * or renaming packages.
 * <p>
 * Reflection is used for version detection ({@link #detectServerVersion})
 * to avoid hard dependencies on either fork's Server class internals.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit uses Paperweight per-MC-version adapters; Nukkit uses runtime fork detection</li>
 *   <li>Block state bit widths differ (MOT=13, NKX=6)</li>
 *   <li>Layer access uses int (MOT) vs BlockLayer enum (NKX)</li>
 * </ul>
 *
 * @see NukkitImplLoader
 * @see com.fastasyncworldedit.nukkit.adapter.mot.MotNukkitAdapter
 * @see com.fastasyncworldedit.nukkit.adapter.nkx.NkxNukkitAdapter
 */
public interface NukkitImplAdapter {

    /**
     * Platform name for display: "Nukkit-MOT" or "NKX".
     */
    String getPlatformName();

    /**
     * Runtime platform version or platform identifier if exact version metadata is unavailable.
     */
    default String getVersion() {
        return getPlatformName();
    }

    /**
     * Granular features supported by this runtime Nukkit platform.
     */
    default Set<NukkitPlatformCapabilities> getCapabilities() {
        return Collections.emptySet();
    }

    /**
     * Returns whether this runtime Nukkit platform supports the given capability.
     */
    default boolean supports(NukkitPlatformCapabilities capability) {
        return getCapabilities().contains(capability);
    }

    /**
     * Reflectively reads common Nukkit server version fields without binding adapters to a specific API fork.
     */
    static String detectServerVersion(String fallback) {
        try {
            Class<?> serverClass = Class.forName("cn.nukkit.Server");
            Object server = serverClass.getMethod("getInstance").invoke(null);
            if (server == null) {
                return fallback;
            }
            String name = invokeString(serverClass, server, "getName");
            String version = invokeString(serverClass, server, "getVersion");
            String nukkitVersion = invokeString(serverClass, server, "getNukkitVersion");
            StringBuilder builder = new StringBuilder(name != null && !name.isBlank() ? name : fallback);
            if (version != null && !version.isBlank()) {
                builder.append(' ').append(version);
            }
            if (nukkitVersion != null && !nukkitVersion.isBlank() && !nukkitVersion.equals(version)) {
                builder.append(" (").append(nukkitVersion).append(')');
            }
            return builder.toString();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static String invokeString(Class<?> type, Object instance, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            Object value = method.invoke(instance);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    /**
     * Runtime value of {@code Block.DATA_BITS} (MOT=13, NKX=6).
     */
    int getBlockDataBits();

    /**
     * Runtime value of {@code Block.DATA_MASK}: {@code (1 << getBlockDataBits()) - 1}.
     */
    default int getBlockDataMask() {
        return (1 << getBlockDataBits()) - 1;
    }

    /**
     * Get the player's language code string, or {@code null} if unsupported (NKX).
     */
    @Nullable
    String getPlayerLanguageCode(Player player);

    /**
     * Get the runtime block ID for sending fake blocks to a player.
     *
     * @param player  target player
     * @param blockId legacy block ID
     * @param meta    legacy block meta
     * @return block runtime ID
     */
    int getBlockRuntimeId(Player player, int blockId, int meta);

    /**
     * Get the entity type identifier string (e.g. "minecraft:creeper").
     * Returns {@code null} if unavailable.
     */
    @Nullable
    String getEntityIdentifier(Entity entity);

    /**
     * Load the block palette from Nukkit's legacy mapper.
     */
    List<NbtMap> loadBlockPalette();

    /**
     * Get {@link BlockStateSnapshot} from Nukkit's {@code BlockStateMapping} for the given NBT state.
     * Returns {@code null} if no mapping exists.
     */
    @Nullable
    BlockStateSnapshot getBlockStateSnapshot(NbtMap nbtState);

    /**
     * Generate a tree whose class hierarchy differs between MOT and NKX.
     * Handles: Mangrove, Cherry, PaleOak.
     *
     * @return {@code true} if the tree was generated, {@code false} if the type is unsupported
     */
    boolean generateTree(String treeType, Level level, int x, int y, int z, NukkitRandom random, Vector3 pos);

    /**
     * Get block ID at the given layer.
     * MOT uses {@code int} layer, NKX uses {@code BlockLayer} enum.
     */
    int getBlockId(FullChunk chunk, int x, int y, int z, int layer);

    /**
     * Set full block ID at the given layer.
     * MOT uses {@code int} layer, NKX uses {@code BlockLayer} enum.
     */
    void setFullBlockId(FullChunk chunk, int x, int y, int z, int layer, int fullId);

    /**
     * Get a UUID for the given entity.
     * MOT: all entities have {@code Entity.getUniqueId()}.
     * NKX: only {@code EntityHuman} has it; for other entities, derive from {@code Entity.getId()}.
     */
    UUID getEntityUUID(Entity entity);

}
