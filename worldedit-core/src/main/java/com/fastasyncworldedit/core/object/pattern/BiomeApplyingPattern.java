package com.fastasyncworldedit.core.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

public class BiomeApplyingPattern extends AbstractExtentPattern {
    private final BiomeType biomeType;

    public BiomeApplyingPattern(Extent extent, BiomeType biomeType) {
        super(extent);
        this.biomeType = biomeType;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        getExtent().setBiome(position, this.biomeType);
        // don't change the block, everything should remain the same
        return getExtent().getFullBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBiome(set, this.biomeType);
    }
}
