package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.util.TextureHolder;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class AngleColorPattern extends AnglePattern {

    protected transient TextureHolder holder;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent   extent to set to
     * @param holder   {@link TextureHolder} to use to get textures
     * @param distance distance to use to calculate angle
     */
    public AngleColorPattern(Extent extent, TextureHolder holder, int distance) {
        super(extent, distance);
        this.holder = holder.getTextureUtil();
    }

    private int getColor(int color, int slope) {
        if (slope == 0) {
            return color;
        }
        double newFactor = (1 - Math.min(1, slope * factor));
        int newRed = (int) (((color >> 16) & 0xFF) * newFactor);
        int newGreen = (int) (((color >> 8) & 0xFF) * newFactor);
        int newBlue = (int) (((color >> 0) & 0xFF) * newFactor);
        return (((color >> 24) & 0xFF) << 24) + (newRed << 16) + (newGreen << 8) + (newBlue << 0);
    }

    @Override
    public <T extends BlockStateHolder<T>> int getSlope(T block, BlockVector3 vector, Extent extent) {
        int slope = super.getSlope(block, vector, extent);
        if (slope != -1) {
            int x = vector.getBlockX();
            int y = vector.getBlockY();
            int z = vector.getBlockZ();
            int height = extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
            if (height > minY) {
                BlockState below = extent.getBlock(x, height - 1, z);
                if (!below.getBlockType().getMaterial().isMovementBlocker()) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return slope;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BaseBlock block = extent.getFullBlock(position);
        int slope = getSlope(block, position, extent);
        if (slope == -1) {
            return block;
        }
        BlockType type = block.getBlockType();
        int color;
        if (type == BlockTypes.GRASS_BLOCK) {
            color = holder.getTextureUtil().getColor(extent.getBiome(position));
        } else {
            color = holder.getTextureUtil().getColor(type);
        }
        if (color == 0) {
            return block;
        }
        int newColor = getColor(color, slope);
        return holder.getTextureUtil().getNearestBlock(newColor).getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockState block = get.getBlock(extent);
        int slope = getSlope(block, get, extent);
        if (slope == -1) {
            return false;
        }
        BlockType type = block.getBlockType();
        int color;
        if (type == BlockTypes.GRASS_BLOCK) {
            color = holder.getTextureUtil().getColor(extent.getBiome(get));
        } else {
            color = holder.getTextureUtil().getColor(type);
        }
        if (color == 0) {
            return false;
        }
        int newColor = getColor(color, slope);
        BlockType newBlock = holder.getTextureUtil().getNearestBlock(newColor);
        if (newBlock == null) {
            return false;
        }
        return set.setBlock(extent, newBlock.getDefaultState());
    }

}
