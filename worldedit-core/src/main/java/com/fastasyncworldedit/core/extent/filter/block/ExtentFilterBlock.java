package com.fastasyncworldedit.core.extent.filter.block;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

public class ExtentFilterBlock extends AbstractFilterBlock {

    private final Extent extent;
    private BlockVector3 pos;

    public ExtentFilterBlock(Extent extent) {
        this.extent = extent;
        this.pos = BlockVector3.ZERO;
    }

    public ExtentFilterBlock init(BlockVector3 pos) {
        this.pos = pos;
        return this;
    }

    @Override
    public BaseBlock getFullBlock() {
        return pos.getFullBlock(extent);
    }

    @Override
    public void setFullBlock(BaseBlock block) {
        pos.setFullBlock(extent, block);
    }

    @Override
    public BiomeType getBiome() {
        return pos.getBiome(extent);
    }

    @Override
    public void setBiome(final BiomeType type) {
        pos.setBiome(extent, type);
    }

    @Override
    public BlockVector3 getPosition() {
        return pos;
    }

    @Override
    public Extent getExtent() {
        return extent;
    }

}
