package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.*;
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
    public Map<BlockVector3, CompoundTag> getTiles() {
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        List<? extends Entity> result = extent.getEntities(new CuboidRegion(BlockVector3.at(bx, 0, bz), BlockVector3.at(bx + 15, 255, bz + 15)));
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<CompoundTag> set = new HashSet<>(result.size());
        for (Entity entity : result) {
            set.add(entity.getState().getNbtData());
        }
        return set;
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        long checkMost = uuid.getMostSignificantBits();
        long checkLeast = uuid.getLeastSignificantBits();
        for (CompoundTag entityTag : getEntities()) {
            long entMost = entityTag.getLong("UUIDMost");
            if (entMost == checkMost) {
                long entLeast = entityTag.getLong("UUIDLeast");
                if (entLeast == checkLeast) {
                    return entityTag;
                }
            }
        }
        return null;
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
        Map<BlockVector3, CompoundTag> tiles = set.getTiles();
        if (!tiles.isEmpty()) {
            for (Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                BlockVector3 pos = entry.getKey();
                extent.setTile(bx + pos.getX(), pos.getY(), bz + pos.getZ(), entry.getValue());
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
    public char[] getArray(int layer) {
        return new char[0];
    }

    @Override
    public IBlocks reset() {
        return null;
    }
}
