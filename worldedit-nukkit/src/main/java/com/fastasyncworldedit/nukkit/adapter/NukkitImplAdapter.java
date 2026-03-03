package com.fastasyncworldedit.nukkit.adapter;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.List;

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
}
