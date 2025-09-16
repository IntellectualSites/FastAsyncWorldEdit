package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.util.TextureHolder;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class DesaturatePattern extends AbstractPattern {

    private final transient TextureUtil util;
    private final transient Extent extent;
    private final double value;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     * @param holder {@link TextureHolder} to use for textures
     * @param value  decimal percent to desaturate by (0 -> 1)
     */
    public DesaturatePattern(Extent extent, TextureHolder holder, double value) {
        this.extent = extent;
        this.util = holder.getTextureUtil();
        this.value = Math.max(0, Math.min(1, value));
    }

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     * @param value  decimal percent to desaturate by (0 -> 1)
     * @param util   {@link TextureUtil} to use for textures
     * @since 2.13.0
     */
    private DesaturatePattern(Extent extent, double value, TextureUtil util) {
        this.extent = extent;
        this.util = util;
        this.value = Math.max(0, Math.min(1, value));
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BlockType type = extent.getBlock(position).getBlockType();
        int color;
        if (type == BlockTypes.GRASS_BLOCK) {
            color = util.getColor(extent.getBiome(position));
        } else {
            color = util.getColor(type);
        }
        return util.getNearestBlock(color).getDefaultState().toBaseBlock();
    }

    private int getColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        double l = 0.3f * r + 0.6f * g + 0.1f * b;
        int red = (int) (r + value * (l - r));
        int green = (int) (g + value * (l - g));
        int blue = (int) (b + value * (l - b));
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType type = get.getBlock(extent).getBlockType();
        int color;
        if (type == BlockTypes.GRASS_BLOCK) {
            color = util.getColor(extent.getBiome(get));
        } else {
            color = util.getColor(type);
        }
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

    @Override
    public Pattern fork() {
        return new DesaturatePattern(extent, value, util);
    }

}
