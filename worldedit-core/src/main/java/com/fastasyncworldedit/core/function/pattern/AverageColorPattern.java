package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TextureHolder;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.Color;

public class AverageColorPattern extends AbstractExtentPattern {

    private final transient TextureHolder holder;
    private final int color;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     * @param holder {@link TextureHolder} to use to get textures
     * @param r      red channel, clamped 0 -> 255
     * @param g      green channel, clamped 0 -> 255
     * @param b      blue channel, clamped 0 -> 255
     * @param a      alpha channel, clamped 0 -> 255
     */
    public AverageColorPattern(Extent extent, TextureHolder holder, int r, int g, int b, int a) {
        super(extent);
        this.holder = holder;
        this.color = new Color(MathMan.clamp(r, 0, 255), MathMan.clamp(g, 0, 255), MathMan.clamp(b, 0, 255), MathMan.clamp(a, 0
                , 255)).getRGB();
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BaseBlock block = getExtent().getFullBlock(position);
        TextureUtil util = holder.getTextureUtil();
        BlockType type = block.getBlockType();
        int currentColor;
        if (type == BlockTypes.GRASS_BLOCK) {
            currentColor = holder.getTextureUtil().getColor(getExtent().getBiome(position));
        } else {
            currentColor = holder.getTextureUtil().getColor(type);
        }
        int newColor = TextureUtil.averageColor(currentColor, color);
        return util.getNearestBlock(newColor).getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType blockType = get.getBlock(extent).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int currentColor;
        if (blockType == BlockTypes.GRASS_BLOCK) {
            currentColor = holder.getTextureUtil().getColor(extent.getBiome(get));
        } else {
            currentColor = holder.getTextureUtil().getColor(blockType);
        }
        if (currentColor == 0) {
            return false;
        }
        int newColor = TextureUtil.averageColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock == blockType) {
            return false;
        }
        return set.setBlock(extent, newBlock.getDefaultState());
    }

}
