package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;
import com.boydti.fawe.bukkit.v1_13.beta.SingleThreadQueueExtent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public interface IDelegateChunk<T extends IChunk> extends IChunk {
    T getParent();

    default IChunk getRoot() {
        IChunk root = getParent();
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
        }
        return root;
    }

    @Override
    default void setBiome(int x, int z, BiomeType biome) {
        getParent().setBiome(x, z, biome);
    }

    @Override
    default void setBlock(int x, int y, int z, BlockStateHolder holder) {
        getParent().setBlock(x, y, z, holder);
    }

    @Override
    default BiomeType getBiome(int x, int z) {
        return getParent().getBiome(x, z);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        return getParent().getBlock(x, y, z);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    default void init(SingleThreadQueueExtent extent, int X, int Z) {
        getParent().init(extent, X, Z);
    }

    @Override
    default int getX() {
        return getParent().getX();
    }

    @Override
    default int getZ() {
        return getParent().getZ();
    }

    @Override
    default void apply() {
        getParent().apply();
    }

    default IChunk findParent(Class<?> clazz) {
        IChunk root = getParent();
        if (clazz.isAssignableFrom(root.getClass())) return root;
        while (root instanceof IDelegateChunk) {
            root = ((IDelegateChunk) root).getParent();
            if (clazz.isAssignableFrom(root.getClass())) return root;
        }
        return null;
    }

}
