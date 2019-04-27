package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class InitChunk extends DelegateChunk<ChunkHolder> {
    public InitChunk(ChunkHolder parent) {
        super(parent);
    }

    protected void init() {
        getParent().setParent(new SetChunk(getParent()));
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        init();
        super.setBiome(x, z, biome);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockStateHolder holder) {
        init();
        super.setBlock(x, y, z, holder);
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        init();
        return super.getBiome(x, z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        init();
        return super.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        init();
        return super.getFullBlock(x, y, z);
    }
}
