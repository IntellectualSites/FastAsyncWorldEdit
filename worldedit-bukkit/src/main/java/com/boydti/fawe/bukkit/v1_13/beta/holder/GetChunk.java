package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IGetBlocks;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class GetChunk extends InitChunk {
    private final IGetBlocks get;

    public GetChunk(ChunkHolder parent) {
        super(parent);
        this.get = parent.get();
    }

    protected void init() {
        getParent().setParent(new FullChunk(getParent(), get, null));
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return get.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        return get.getBiome(x, z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return get.getBlock(x, y, z);
    }
}
