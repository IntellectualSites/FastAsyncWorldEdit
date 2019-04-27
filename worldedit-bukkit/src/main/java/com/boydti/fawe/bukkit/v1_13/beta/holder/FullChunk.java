package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IGetBlocks;
import com.boydti.fawe.bukkit.v1_13.beta.ISetBlocks;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class FullChunk extends DelegateChunk<ChunkHolder> {
    private final ISetBlocks set;
    private final IGetBlocks get;

    public FullChunk(ChunkHolder parent, IGetBlocks get, ISetBlocks set) {
        super(parent);
        this.set = set == null ? parent.set() : set;
        this.get = get == null ? parent.get() : get;
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

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        set.setBiome(x, z, biome);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockStateHolder holder) {
        set.setBlock(x, y, z, holder);
    }
}