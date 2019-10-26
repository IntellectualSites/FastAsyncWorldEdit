package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Map;
import java.util.Set;

/**
 * Shared interface for IGetBlocks and ISetBlocks
 */
public interface IBlocks extends Trimable {

    boolean hasSection(int layer);

    char[] getArray(int layer);

    BlockState getBlock(int x, int y, int z);

    Map<BlockVector3, CompoundTag> getTiles();

    Set<CompoundTag> getEntities();

    BiomeType getBiomeType(int x, int z);

    default int getBitMask() {
        int mask = 0;
        for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
            if (hasSection(layer));
            mask |= (1 << layer);
        }
        return mask;
    }

    IBlocks reset();
}
