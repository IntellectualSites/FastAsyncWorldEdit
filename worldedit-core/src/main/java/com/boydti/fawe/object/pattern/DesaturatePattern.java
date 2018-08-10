package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.io.IOException;

public class DesaturatePattern extends AbstractPattern {
    private transient TextureHolder holder;
    private final Extent extent;
    private final double value;

    public DesaturatePattern(Extent extent, double value, TextureHolder util) {
        this.extent = extent;
        this.holder = util;
        this.value = Math.max(0, Math.min(1, value));
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        BlockType block = extent.getBlockType(position);
        TextureUtil util = holder.getTextureUtil();
        int color = util.getColor(block);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        double l = 0.3f * r + 0.6f * g + 0.1f * b;
        int red = (int) (r + value * (l - r));
        int green = (int) (g + value * (l - g));
        int blue = (int) (b + value * (l - b));
        int newColor = (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
        return util.getNearestBlock(newColor).getDefaultState();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition) throws WorldEditException {
        BlockType block = extent.getBlockType(getPosition);
        TextureUtil util = holder.getTextureUtil();
        int color = util.getColor(block);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        double l = 0.3f * r + 0.6f * g + 0.1f * b;
        int red = (int) (r + value * (l - r));
        int green = (int) (g + value * (l - g));
        int blue = (int) (b + value * (l - b));
        int newColor = (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
        if (newColor == color) {
            return false;
        }
        BlockType newBlock = util.getNextNearestBlock(newColor);
        if (block.equals(newBlock)) {
            return false;
        }
        return extent.setBlock(setPosition, newBlock.getDefaultState());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        holder = Fawe.get().getCachedTextureUtil(true, 0, 100);
    }
}