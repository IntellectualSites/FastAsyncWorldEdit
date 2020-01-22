package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CombinedBlocks implements IBlocks {
    private final IBlocks secondary;
    private final IBlocks primary;
    private final int addMask;

    /**
     * @param secondary
     * @param primary
     * @param addMask - bitMask for force sending sections, else 0 to send the primary ones
     */
    public CombinedBlocks(IBlocks secondary, IBlocks primary, int addMask) {
        this.secondary = secondary;
        this.primary = primary;
        this.addMask = addMask == 0 ? 0 : addMask & secondary.getBitMask();
    }

    @Override
    public int getBitMask() {
        int bitMask = addMask;
        for (int layer = 0; layer < FaweCache.chunkLayers; layer++) {
            if (primary.hasSection(layer)) {
                bitMask |= (1 << layer);
            }
        }
        return bitMask;
    }

    @Override
    public boolean hasSection(int layer) {
        return primary.hasSection(layer) || secondary.hasSection(layer);
    }

    @Override
    public char[] load(int layer) {
        if (primary.hasSection(layer)) {
            char[] blocks = primary.load(layer);
            if (secondary.hasSection(layer) && primary != secondary) {
                int i = 0;
                for (; i < 4096; i++) {
                    if (blocks[i] == 0) {
                        break;
                    }
                }
                if (i != 4096) {
                    char[] fallback = secondary.load(layer);
                    for (; i < 4096; i++) {
                        if (blocks[i] == 0) {
                            blocks[i] = fallback[i];
                        }
                    }
                }
            }
            return blocks;
        }
        return secondary.load(layer);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        BlockState block = primary.getBlock(x, y, z);
        if (block.getBlockType() == BlockTypes.__RESERVED__) {
            return secondary.getBlock(x, y, z);
        }
        return block;
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        Map<BlockVector3, CompoundTag> tiles = primary.getTiles();
        if (primary != secondary) {
            if (tiles.isEmpty()) {
                return secondary.getTiles();
            }
            Map<BlockVector3, CompoundTag> otherTiles = secondary.getTiles();
            if (!otherTiles.isEmpty()) {
                HashMap<BlockVector3, CompoundTag> copy = null;
                for (Map.Entry<BlockVector3, CompoundTag> entry : otherTiles.entrySet()) {
                    BlockVector3 pos = entry.getKey();
                    BlockState block = primary.getBlock(pos.getX(), pos.getY(), pos.getZ());
                    if (block.getBlockType() == BlockTypes.__RESERVED__) {
                        if (copy == null) copy = new HashMap<>(tiles);
                        copy.put(pos, entry.getValue());
                    }
                }
                if (copy != null) return copy;
            }
        }
        return tiles;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        CompoundTag tile = primary.getTile(x, y, z);
        if (tile != null) {
            return tile;
        }
        return secondary.getTile(x, y, z);
    }

    @Override
    public Set<CompoundTag> getEntities() {
        Set<CompoundTag> joined = primary.getEntities();
        if (primary != secondary) {
            Set<CompoundTag> ents2 = secondary.getEntities();
            if (joined.isEmpty()) return ents2;
            if (ents2.isEmpty()) return joined;
            joined = new HashSet<>(joined);
            joined.addAll(ents2);
        }
        return joined;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BiomeType biome = primary.getBiomeType(x, y, z);
        if (biome == null) {
            return secondary.getBiomeType(x, y, z);
        }
        return biome;
    }

    @Override
    public IBlocks reset() {
        return null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }
}
