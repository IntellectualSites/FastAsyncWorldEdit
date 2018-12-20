package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.DataAnglePattern;
import com.boydti.fawe.util.TextureHolder;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.io.IOException;

public class AngleColorPattern extends DataAnglePattern {
    protected transient TextureHolder util;

    public AngleColorPattern(Extent extent, TextureHolder util, int distance) {
        super(extent, distance);
        this.util = util;
    }

    public int getColor(int color, int slope) {
        if (slope == 0) return color;
        double newFactor = (1 - Math.min(1, slope * FACTOR));
        int newRed = (int) (((color >> 16) & 0xFF) * newFactor);
        int newGreen = (int) (((color >> 8) & 0xFF) * newFactor);
        int newBlue = (int) (((color >> 0) & 0xFF) * newFactor);
        return (((color >> 24) & 0xFF) << 24) + (newRed << 16) + (newGreen << 8) + (newBlue << 0);
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        BlockStateHolder block = extent.getBlock(position);
        int slope = getSlope(block, position);
        if (slope == -1) return block;
        int color = util.getTextureUtil().getColor(block.getBlockType());
        if (color == 0) return block;
        int newColor = getColor(color, slope);
        return util.getTextureUtil().getNearestBlock(newColor).getDefaultState();
    }

    @Override
    public int getSlope(BlockStateHolder block, Vector vector) {
        int slope = super.getSlope(block, vector);
        if (slope != -1) {
            int x = vector.getBlockX();
            int y = vector.getBlockY();
            int z = vector.getBlockZ();
            int height = extent.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
            if (height > 0) {
                BlockStateHolder below = extent.getLazyBlock(x, height - 1, z);
                if (!below.getBlockType().getMaterial().isMovementBlocker()) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return slope;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        BlockStateHolder block = extent.getBlock(getPosition);
        int slope = getSlope(block, getPosition);
        if (slope == -1) return false;
        int color = util.getTextureUtil().getColor(block.getBlockType());
        if (color == 0) return false;
        int newColor = getColor(color, slope);
        BlockType newBlock = util.getTextureUtil().getNearestBlock(newColor);
        if (newBlock == null) return false;
        return extent.setBlock(setPosition, newBlock.getDefaultState());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        util = Fawe.get().getCachedTextureUtil(true, 0, 100);
    }
}