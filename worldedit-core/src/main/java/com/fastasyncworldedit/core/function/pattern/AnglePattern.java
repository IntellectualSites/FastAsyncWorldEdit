package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.extent.ExtentHeightCacher;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public abstract class AnglePattern extends AbstractPattern {

    public final double factor;
    public final Extent extent;
    public final int maxY;
    public final int minY;
    public final int distance;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent   extent to set to
     * @param distance distance to calculate angle with
     */
    public AnglePattern(Extent extent, int distance) {
        this.extent = new ExtentHeightCacher(extent);
        this.maxY = extent.getMaxY();
        this.minY = extent.getMinY();
        this.distance = distance;
        this.factor = (1D / distance) * (1D / maxY);
    }

    public <T extends BlockStateHolder<T>> int getSlope(T block, BlockVector3 vector, Extent extent) {
        int x = vector.x();
        int y = vector.y();
        int z = vector.z();
        if (!block.getBlockType().getMaterial().isMovementBlocker()) {
            return -1;
        }
        int slope = Math.abs(extent.getNearestSurfaceTerrainBlock(x + distance, z, y, minY, maxY) -
                extent.getNearestSurfaceTerrainBlock(x - distance, z, y, minY, maxY)) * 7;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x, z + distance, y, minY, maxY) -
                extent.getNearestSurfaceTerrainBlock(x, z - distance, y, minY, maxY)) * 7;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x + distance, z + distance, y, minY, maxY) -
                extent.getNearestSurfaceTerrainBlock(x - distance, z - distance, y, minY, maxY)) * 5;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x - distance, z + distance, y, minY, maxY) -
                extent.getNearestSurfaceTerrainBlock(x + distance, z - distance, y, minY, maxY)) * 5;
        return slope;
    }

    @Override
    public abstract BaseBlock applyBlock(BlockVector3 position);

    @Override
    public abstract boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition);

}
