package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

public class BiomeApplyingPattern extends AbstractExtentPattern {

    private final BiomeType biomeType;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent    extent to set to
     * @param biomeType biome type to set
     */
    public BiomeApplyingPattern(Extent extent, BiomeType biomeType) {
        super(extent);
        this.biomeType = biomeType;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        getExtent().setBiome(position, this.biomeType);
        // don't change the block, everything should remain the same
        return getExtent().getFullBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBiome(set, this.biomeType);
    }

}
