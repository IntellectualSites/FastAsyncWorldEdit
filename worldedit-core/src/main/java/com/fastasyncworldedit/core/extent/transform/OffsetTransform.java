package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class OffsetTransform extends ResettableExtent {

    private final int dx;
    private final int dy;
    private final int dz;

    /**
     * New instance
     *
     * @param parent extent to set to
     * @param dx     offset x
     * @param dy     offset y
     * @param dz     offset z
     */
    public OffsetTransform(Extent parent, int dx, int dy, int dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public boolean setBiome(BlockVector3 location, BiomeType biome) {
        int x = location.x() + dx;
        int y = location.x() + dy;
        int z = location.x() + dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        x += dx;
        y += dy;
        z += dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block)
            throws WorldEditException {
        int x = location.x() + dx;
        int y = location.x() + dy;
        int z = location.x() + dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        x += dx;
        y += dy;
        z += dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBlock(x, y, z, block);
    }

}
