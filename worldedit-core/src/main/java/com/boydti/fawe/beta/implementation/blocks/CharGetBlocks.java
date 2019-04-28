package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IGetBlocks;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class CharGetBlocks extends CharBlocks implements IGetBlocks {

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        return null;
    }

    @Override
    public BiomeType getBiome(final int x, final int z) {
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        return null;
    }
}
