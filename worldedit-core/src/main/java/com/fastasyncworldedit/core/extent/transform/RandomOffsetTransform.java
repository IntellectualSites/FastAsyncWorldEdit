package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.SplittableRandom;

public class RandomOffsetTransform extends ResettableExtent {

    private final int dx;
    private final int dy;
    private final int dz;
    private transient SplittableRandom random;

    /**
     * New instance
     *
     * @param parent extent to set to
     * @param dx     range of x values to choose from (0 -> x)
     * @param dy     range of y values to choose from (0 -> y)
     * @param dz     range of z values to choose from (0 -> z)
     */
    public RandomOffsetTransform(Extent parent, int dx, int dy, int dz) {
        super(parent);
        this.dx = dx + 1;
        this.dy = dy + 1;
        this.dz = dz + 1;
        this.random = new SplittableRandom();
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        int x = position.x() + random.nextInt(1 + (dx << 1)) - dx;
        int y = position.y() + random.nextInt(1 + (dy << 1)) - dy;
        int z = position.z() + random.nextInt(1 + (dz << 1)) - dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        x = x + random.nextInt(1 + (dx << 1)) - dx;
        y = y + random.nextInt(1 + (dy << 1)) - dy;
        z = z + random.nextInt(1 + (dz << 1)) - dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block)
            throws WorldEditException {
        int x = pos.x() + random.nextInt(1 + (dx << 1)) - dx;
        int y = pos.y() + random.nextInt(1 + (dy << 1)) - dy;
        int z = pos.z() + random.nextInt(1 + (dz << 1)) - dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        x = x + random.nextInt(1 + (dx << 1)) - dx;
        y = y + random.nextInt(1 + (dy << 1)) - dy;
        z = z + random.nextInt(1 + (dz << 1)) - dz;
        if (!getExtent().contains(x, y, z)) {
            return false;
        }
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        random = new SplittableRandom();
        return super.setExtent(extent);
    }

}
