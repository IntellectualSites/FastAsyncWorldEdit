package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.ISetBlocks;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CharSetBlocks extends CharBlocks implements ISetBlocks {
    private BiomeType[] biomes;
    private HashMap<Short, CompoundTag> tiles;
    private HashSet<CompoundTag> entities;
    private HashSet<UUID> entityRemoves;

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[256];
        }
        biomes[x + (z << 4)] = biome;
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder holder) {
        set(x, y, z, holder.getOrdinalChar());
        return true;
    }

    @Override
    public boolean isEmpty() {
        if (biomes != null) return false;
        for (int i = 0; i < 16; i++) {
            if (hasSection(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset() {
        biomes = null;
        super.reset();
    }
}