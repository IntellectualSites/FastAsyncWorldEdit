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
import java.util.List;
import java.util.UUID;

/**
 * Adapter interface abstracting API differences between Nukkit-MOT and NKX.
 * <p>
 * Each platform provides its own implementation compiled against its specific API,
 * loaded at runtime by {@link NukkitImplLoader}.
 */
public interface NukkitImplAdapter {

    /**
     * Platform name for display: "Nukkit-MOT" or "NKX".
     */
    String getPlatformName();

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
