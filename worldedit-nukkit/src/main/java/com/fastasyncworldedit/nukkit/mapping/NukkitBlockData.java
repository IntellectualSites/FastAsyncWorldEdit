package com.fastasyncworldedit.nukkit.mapping;

import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;

/**
 * Represents a Nukkit block as its legacy ID and metadata.
 */
public record NukkitBlockData(int blockId, int metadata) {

    public static final NukkitBlockData AIR = new NukkitBlockData(0, 0);

    /**
     * Get the Nukkit full block ID: (blockId &lt;&lt; DATA_BITS) | metadata
     */
    public int getFullId() {
        return (blockId << NukkitImplLoader.get().getBlockDataBits()) | metadata;
    }

}
