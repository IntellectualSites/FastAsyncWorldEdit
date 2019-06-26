package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.DelegateChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class RegionCharSetBlocks implements DelegateChunkSet {
    private final Region region;
    private final CharSetBlocks parent;
    private final int X, Z;

    public RegionCharSetBlocks(Region region, int X, int Z, CharSetBlocks parent) {
        this.region = region;
        this.parent = parent;
        this.X = X;
        this.Z = Z;
    }

    @Override
    public CharSetBlocks getParent() {
        return parent;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (region.contains(x, y, z)) {
            return parent.setBiome(x, y, z, biome);
        }
        return false;
    }

    public static final CharBlocks.Section EMPTY_REGION = new CharBlocks.Section() {
        @Override
        public final char[] get(final CharBlocks blocks, final int layer) {
            RegionCharSetBlocks checked = (RegionCharSetBlocks) blocks;
            Region region = checked.getRegion();

            blocks.sections[layer] = FULL;
            char[] arr = blocks.blocks[layer];
            if (arr == null) {
                arr = blocks.blocks[layer] = blocks.load(layer);
            } else {
                blocks.blocks[layer] = blocks.load(layer, arr);
            }
            return arr;
        }
    };

    public static final CharBlocks.Section NULL = new CharBlocks.Section() {
        @Override
        public final char[] get(final CharBlocks blocks, final int layer) {
            return FaweCache.EMPTY_CHAR_4096;
        }
    };

    // EMPTY_CHAR_4096

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockStateHolder holder) {

    }

    @Override
    public void setTile(final int x, final int y, final int z, final CompoundTag tile) {
        if (region.contains(x, y, z)) {
            super.setTile(x, y, z, tile);
        }
    }
}
