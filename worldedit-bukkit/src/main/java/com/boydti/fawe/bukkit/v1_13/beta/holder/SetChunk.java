package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;
import com.boydti.fawe.bukkit.v1_13.beta.ISetBlocks;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class SetChunk extends InitChunk {

    private final ISetBlocks set;

    public SetChunk(ChunkHolder parent) {
        super(parent);
        this.set = parent.set();
    }

    protected void init() {
        getParent().setParent(new FullChunk(getParent(), null, set));
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        set.setBiome(x, z, biome);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockStateHolder holder) {
        set.setBlock(x, y, z, holder);
    }
}
