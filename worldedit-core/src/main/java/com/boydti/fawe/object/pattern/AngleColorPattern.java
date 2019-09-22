package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.DataAnglePattern;
import com.boydti.fawe.util.TextureHolder;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

public class AngleColorPattern extends DataAnglePattern {
    protected transient TextureHolder holder;

    public AngleColorPattern(Extent extent, TextureHolder holder, int distance) {
        super(extent, distance);
        this.holder = holder.getTextureUtil();
    }

    public int getColor(int color, int slope) {
        if (slope == 0) return color;
        double newFactor = (1 - Math.min(1, slope * factor));
        int newRed = (int) (((color >> 16) & 0xFF) * newFactor);
        int newGreen = (int) (((color >> 8) & 0xFF) * newFactor);
        int newBlue = (int) (((color >> 0) & 0xFF) * newFactor);
        return (((color >> 24) & 0xFF) << 24) + (newRed << 16) + (newGreen << 8) + (newBlue << 0);
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BaseBlock block = extent.getFullBlock(position);
        int slope = getSlope(block, position, extent);
        if (slope == -1) return block;
        int color = holder.getTextureUtil().getColor(block.getBlockType());
        if (color == 0) return block;
        int newColor = getColor(color, slope);
        return holder.getTextureUtil().getNearestBlock(newColor).getDefaultState().toBaseBlock();
    }

    @Override
    public <T extends BlockStateHolder<T>> int getSlope(T block, BlockVector3 vector, Extent extent) {
        int slope = super.getSlope(block, vector, extent);
        if (slope != -1) {
            int x = vector.getBlockX();
            int y = vector.getBlockY();
            int z = vector.getBlockZ();
            int height = extent.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
            if (height > 0) {
                BlockState below = extent.getBlock(x, height - 1, z);
                if (!below.getBlockType().getMaterial().isMovementBlocker()) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return slope;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockState block = get.getBlock(extent);
        int slope = getSlope(block, get, extent);
        if (slope == -1) return false;
        int color = holder.getTextureUtil().getColor(block.getBlockType());
        if (color == 0) return false;
        int newColor = getColor(color, slope);
        BlockType newBlock = holder.getTextureUtil().getNearestBlock(newColor);
        if (newBlock == null) return false;
        return set.setBlock(extent, newBlock.getDefaultState());
    }
}
