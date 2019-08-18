package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class FallbackChunkGet implements IChunkGet {
    private final int bx, bz;
    private final Extent extent;

    public FallbackChunkGet(Extent extent, int chunkX, int chunkZ) {
        this.extent = extent;
        this.bx = chunkX << 4;
        this.bz = chunkZ << 4;
    }
    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return extent.getFullBlock(bx + x, y, bz + z);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return extent.getBiomeType(bx + x, bz + z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return extent.getBlock(bx + x, y, bz + z);
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        return extent.getFullBlock(bx + x, y, bz + z).getNbtData();
    }

    @Override
    public boolean trim(boolean aggressive) {
        return true;
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalize) {
        for (int layer = 0; layer < 16; layer++) {
            if (set.hasSection(layer)) {
                char[] arr = set.getArray(layer);
                int by = layer << 4;
                for (int y = 0, i = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++, i++) {
                            char ordinal = arr[i];
                            if (ordinal != 0) {
                                BlockState block = BlockState.getFromOrdinal(ordinal);
                                extent.setBlock(bx + x, by + y, bz + z, block);
                            }
                        }
                    }
                }

            }
        }
        Map<Short, CompoundTag> tiles = set.getTiles();
        if (!tiles.isEmpty()) {
            for (Map.Entry<Short, CompoundTag> entry : tiles.entrySet()) {
                short blockHash = entry.getKey();
                final int x = (blockHash >> 12 & 0xF) + bx;
                final int y = (blockHash & 0xFF);
                final int z = (blockHash >> 8 & 0xF) + bz;
                extent.setTile(bx + x, y, bz + z, entry.getValue());
            }

        }
        Set<CompoundTag> spawns = set.getEntities();
        if (!spawns.isEmpty()) {
            for (CompoundTag spawn : spawns) {
                BaseEntity ent = new BaseEntity(spawn);
                extent.createEntity(ent.getLocation(extent), ent);
            }
        }
        Set<UUID> kills = set.getEntityRemoves();
        if (!kills.isEmpty()) {
            for (UUID kill : kills) {
                extent.removeEntity(0, 0, 0, kill);
            }
        }
        BiomeType[] biomes = set.getBiomes();
        if (biomes != null) {
            for (int z = 0, i = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, i++) {
                    BiomeType biome = biomes[i];
                    if (biome != null) {
                        extent.setBiome(bx + x, 0, bz + z, biome);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public char[] load(int layer) {
        char[] arr = FaweCache.IMP.SECTION_BITS_TO_CHAR.get();
        int by = layer << 4;
        for (int y = 0, i = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, i++) {
                    arr[i] = getBlock(bx + x, by + y, bz + z).getOrdinalChar();
                }
            }
        }
        return arr;
    }

    @Override
    public boolean hasSection(int layer) {
        return true;
    }

    @Override
    public IBlocks reset() {
        return null;
    }
}
