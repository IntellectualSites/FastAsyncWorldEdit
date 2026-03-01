package com.fastasyncworldedit.nukkitmot.mapping;

import cn.nukkit.block.Block;

/**
 * Represents a Nukkit block as its legacy ID and metadata.
 */
public record NukkitBlockData(int blockId, int metadata) {

    public static final NukkitBlockData AIR = new NukkitBlockData(0, 0);

    /**
     * Get the Nukkit full block ID: (blockId &lt;&lt; DATA_BITS) | metadata
     */
    public int getFullId() {
        return (blockId << Block.DATA_BITS) | metadata;
    }

}
