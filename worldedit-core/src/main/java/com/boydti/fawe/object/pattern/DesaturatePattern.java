package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import java.io.IOException;
import java.io.ObjectInputStream;

public class DesaturatePattern extends AbstractPattern {
    private final TextureHolder holder;
    private final Extent extent;
    private final double value;

    public DesaturatePattern(Extent extent, double value, TextureHolder util) {
        this.extent = extent;
        this.holder = util;
        this.value = Math.max(0, Math.min(1, value));
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BlockType block = extent.getBlock(position).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int color = getColor(util.getColor(block));
        return util.getNearestBlock(color).getDefaultState().toBaseBlock();
    }

    public int getColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        double l = 0.3f * r + 0.6f * g + 0.1f * b;
        int red = (int) (r + value * (l - r));
        int green = (int) (g + value * (l - g));
        int blue = (int) (b + value * (l - b));
        int newColor = (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
        return newColor;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType type = get.getBlock(extent).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int color = util.getColor(type);
        int newColor = getColor(color);
        if (newColor == color) {
            return false;
        }
        BlockType newType = util.getNextNearestBlock(newColor);
        if (type.equals(newType)) {
            return false;
        }
        return set.setBlock(extent, newType.getDefaultState());
    }
}
