package com.fastasyncworldedit.nukkit.adapter;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.tag.CompoundTag;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping.NukkitItemData;
import com.fastasyncworldedit.nukkit.mapping.NukkitBlockData;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter interface abstracting API differences between Nukkit-MOT and NKX.
 * <p>
 * Nukkit has multiple active forks with incompatible APIs.
 * Unlike Bukkit, where FAWE compiles separate adapter modules per Minecraft
 * version, Nukkit adapters must be detected and loaded at runtime because
 * these forks share the same package names ({@code cn.nukkit}) but have different
 * classes and methods. Compile-time separation is impossible without shading
 * or renaming packages.
 * <p>
 * Reflection is used for version detection ({@link #detectServerVersion})
 * to avoid hard dependencies on either fork's Server class internals.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit uses Paperweight per-MC-version adapters; Nukkit uses runtime fork detection</li>
 *   <li>Block state representation differs (legacy full IDs vs fork-specific APIs)</li>
 *   <li>Layer access uses int or the BlockLayer enum depending on the fork</li>
 * </ul>
 *
 * @see NukkitImplLoader
 * @see com.fastasyncworldedit.nukkit.adapter.mot.NukkitMOTAdapter
 * @see com.fastasyncworldedit.nukkit.adapter.nkx.NukkitAdapter
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
     * @param fullId  platform-specific full block ID produced by {@link NukkitBlockData#getFullId()}
     * @return block runtime ID
     */
    int getBlockRuntimeId(Player player, int fullId);

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
     * Convert a Bedrock block state NBT entry into the platform-specific block data used by this adapter.
     * Returns {@code null} if no mapping exists.
     */
    @Nullable
    NukkitBlockData createBlockData(NbtMap nbtState);

    /**
     * Get block ID at the given layer.
     * MOT uses {@code int} layer, NKX uses {@code BlockLayer} enum.
     */
    int getFullBlockId(Object chunk, int x, int y, int z, int layer);

    /**
     * Set full block ID at the given layer.
     * MOT uses {@code int} layer, NKX uses {@code BlockLayer} enum.
     */
    void setFullBlockId(Object chunk, int x, int y, int z, int layer, int fullId);

    /**
     * Get or create a chunk object for this platform.
     */
    Object getChunk(Level level, int chunkX, int chunkZ);

    /**
     * Get a biome ID from a chunk. All supported forks store 3D biomes (per x/y/z)
     * in their modern chunk implementations, so the {@code y} coordinate is honored;
     * whether the {@link #THREE_DIMENSIONAL_BIOMES} capability is reported depends on
     * the runtime chunk type backing the level.
     */
    int getChunkBiomeId(Object chunk, int x, int y, int z);

    /**
     * Set a biome ID in a chunk. All supported forks store 3D biomes (per x/y/z)
     * in their modern chunk implementations, so the {@code y} coordinate is honored;
     * whether the {@link #THREE_DIMENSIONAL_BIOMES} capability is reported depends on
     * the runtime chunk type backing the level.
     */
    void setChunkBiomeId(Object chunk, int x, int y, int z, int biomeId);

    /**
     * Get a biome ID from a level.
     */
    int getLevelBiomeId(Level level, int x, int y, int z);

    /**
     * Set a biome ID in a level.
     */
    void setLevelBiomeId(Level level, int x, int y, int z, int biomeId);

    Map<Long, BlockEntity> getBlockEntities(Object chunk);

    @Nullable
    BlockEntity getTile(Object chunk, int x, int y, int z);

    int getBlockSkyLight(Object chunk, int x, int y, int z);

    int getBlockLight(Object chunk, int x, int y, int z);

    void setChunkChanged(Object chunk, boolean changed);

    @Nullable
    BlockEntity createBlockEntity(String id, Object chunk, CompoundTag nbt);

    @Nullable
    Entity createEntity(String id, Object chunk, CompoundTag nbt);

    Map<Long, Entity> getChunkEntities(Level level, int chunkX, int chunkZ);

    /**
     * Convert a live Nukkit block object into the platform-specific full block ID used by mappings.
     */
    int getFullBlockId(Block block);

    /**
     * Convert a platform-specific full block ID into a live Nukkit block object.
     */
    Block getBlock(int fullId);

    /**
     * Returns whether the given platform-specific full block ID represents water.
     */
    boolean isWaterFullId(int fullId);

    /**
     * Returns the platform-specific full block ID for air.
     */
    int getAirFullId();

    /**
     * Returns the platform-specific full block ID for still water.
     */
    int getStillWaterFullId();

    @Nullable
    NukkitItemData createItemData(String bedrockId, int metadata);

    String getItemMappingKey(Item item);

    String getItemMappingKey(NukkitItemData data);

    Item getItem(NukkitItemData data, int amount);

    Item getAirItem();

    boolean matchesItem(Item item, NukkitItemData data);

    boolean isAirItem(Item item);

    /**
     * Simulate using an item on a block. Forks expose incompatible signatures,
     * so adapters override this when their common API diverges.
     */
    default boolean useItemOn(Level level, BlockVector3 position, Item item, Direction face) {
        return level.useItemOn(
                NukkitAdapter.adapt(position),
                item,
                NukkitAdapter.adapt(face),
                0.5f,
                0.5f,
                0.5f
        ) != null;
    }

    /**
     * Get a UUID for the given entity.
     * MOT: all entities have {@code Entity.getUniqueId()}.
     * NKX: only {@code EntityHuman} has it; for other entities, derive from {@code Entity.getId()}.
     */
    UUID getEntityUUID(Entity entity);

    /**
     * Recalculate block and sky light, plus the height map, for the given chunk after a bulk edit.
     * <p>
     * FAWE edits chunks via raw {@code FullChunk.setFullBlockId}, which bypasses the per-block
     * lighting propagation that the vanilla {@code Level.setBlock} path performs. Without an
     * explicit recalculation the chunk's stored {@code skyLight}/{@code blockLight} arrays stay
     * stale and are sent verbatim to Bedrock clients (whose chunk packets carry server-computed
     * light), producing dark patches and glowing artifacts.
     * <p>
     * Each fork exposes a different recalculation surface:
     * <ul>
     *   <li>MOT: {@code FullChunk.recalculateHeightMap()} + {@code populateSkyLight()} + {@code populateBlockLight()}</li>
     *   <li>NKX: {@code recalculateHeightMap()} + {@code populateSkyLight()} (block light is recomputed on send)</li>
     * </ul>
     *
     * @param chunk the chunk object produced by {@link #getChunk}
     */
    void recalculateLight(Object chunk);

    /**
     * Attempt to grow a tree of the given WorldEdit type at the world coordinates. The caller is
     * responsible for capturing the resulting block changes into FAWE history; this method only
     * performs the platform-native placement and reports whether anything was placed.
     * <p>
     * Forks route to their own tree APIs ({@code ObjectTree} on NKX/MOT).
     * Unknown types fall back to a plain oak tree ({@code ObjectTree}/equivalent).
     *
     * @param level the level to place in
     * @param type  the WorldEdit tree type
     * @param x     world x
     * @param y     world y
     * @param z     world z
     * @return {@code true} if the tree was placed
     */
    boolean growTree(Level level, com.sk89q.worldedit.util.TreeGenerator.TreeType type, int x, int y, int z);

}

