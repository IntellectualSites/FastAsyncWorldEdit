package com.boydti.fawe.object;

import com.boydti.fawe.object.extent.ExtentHeightCacher;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class DataAnglePattern extends AbstractPattern {
    public final double factor;
    public final Extent extent;
    public final int maxY;
    public final int distance;

    public DataAnglePattern(Extent extent, int distance) {
        this.extent = new ExtentHeightCacher(extent);
        this.maxY = extent.getMaximumPoint().getBlockY();
        this.distance = distance;
        this.factor = (1D / distance) * (1D / 255);
    }

    public <T extends BlockStateHolder<T>> int getSlope(T block, BlockVector3 vector, Extent extent) {
        int x = vector.getBlockX();
        int y = vector.getBlockY();
        int z = vector.getBlockZ();
        if (!block.getBlockType().getMaterial().isMovementBlocker()) {
            return -1;
        }
        int slope = Math.abs(
            extent.getNearestSurfaceTerrainBlock(x + distance, z, y, 0, maxY) - extent
                .getNearestSurfaceTerrainBlock(x - distance, z, y, 0, maxY)) * 7;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x, z + distance, y, 0, maxY) - extent.getNearestSurfaceTerrainBlock(x, z - distance, y, 0, maxY)) * 7;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x + distance, z + distance, y, 0, maxY) - extent.getNearestSurfaceTerrainBlock(x - distance, z - distance, y, 0, maxY)) * 5;
        slope += Math.abs(extent.getNearestSurfaceTerrainBlock(x - distance, z + distance, y, 0, maxY) - extent.getNearestSurfaceTerrainBlock(x + distance, z - distance, y, 0, maxY)) * 5;
        return slope;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BlockState block = extent.getBlock(position);
        int slope = getSlope(block, position, extent);
        if (slope == -1) return block.toBaseBlock();
        int data = Math.min(slope, 255) >> 4;
        return block.withPropertyId(data).toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition) throws WorldEditException {
        BlockState block = extent.getBlock(getPosition);
        int slope = getSlope(block, getPosition, extent);
        if (slope == -1) return false;
        int data = Math.min(slope, 255) >> 4;
        return extent.setBlock(setPosition, block.withPropertyId(data));
    }
}
