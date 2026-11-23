package com.fastasyncworldedit.nukkit.mapping;

import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;

/**
 * Represents a platform-specific Nukkit block mapping.
 * <p>
 * Legacy Nukkit forks use {@code blockId + metadata}; forks may also use the block state hash directly.
 */
public record NukkitBlockData(int blockId, int metadata, int fullId) {

    public static final NukkitBlockData AIR = legacy(0, 0);

    public static NukkitBlockData legacy(int blockId, int metadata) {
        return new NukkitBlockData(blockId, metadata, -1);
    }

    public static NukkitBlockData fullId(int fullId) {
        return new NukkitBlockData(-1, 0, fullId);
    }

    /**
     * Get the platform-specific full block ID.
     */
    public int getFullId() {
        if (fullId != -1) {
            return fullId;
        }
        return (blockId << NukkitImplLoader.get().getBlockDataBits()) | metadata;
    }

}
