package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.sk89q.worldedit.world.biome.BiomeType;

public class BlocksHelper {

    public Object[] sectionLocks;
    protected int minSectionPosition;
    protected int maxSectionPosition;
    protected int sectionCount;

    static BiomeType getBiomeType(
            final int x,
            final int y,
            final int z,
            final BiomeType[][] biomes,
            final int minSectionPosition,
            final int maxSectionPosition
    ) {
        int layer;
        if (biomes == null || (y >> 4) < minSectionPosition || (y >> 4) > maxSectionPosition) {
            return null;
        } else if (biomes[(layer = (y >> 4) - minSectionPosition)] == null) {
            return null;
        }
        return biomes[layer][(y & 15) >> 2 | (z >> 2) << 2 | x >> 2];
    }

    /**
     * Get the number of stored sections
     */
    public int getSectionCount() {
        return sectionCount;
    }

    public int getMaxSectionPosition() {
        return maxSectionPosition;
    }

    public int getMinSectionPosition() {
        return minSectionPosition;
    }


}
