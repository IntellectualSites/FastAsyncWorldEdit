package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import net.minecraft.server.v1_13_R2.ChunkSection;

public class CharGetBlocks extends CharBlocks implements IGetBlocks {
    private ChunkSection[] sections;

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return null;
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        return null;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return null;
    }
}
