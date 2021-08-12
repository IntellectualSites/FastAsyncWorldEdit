package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TextureHolder;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;

import java.awt.Color;

public class AverageColorPattern extends AbstractExtentPattern {

    private final transient TextureHolder holder;
    private final int color;

    public AverageColorPattern(Extent extent, TextureHolder util, int r, int g, int b, int a) {
        super(extent);
        this.holder = util;
        this.color = new Color(MathMan.clamp(r, 0, 255), MathMan.clamp(g, 0, 255), MathMan.clamp(b, 0, 255), MathMan.clamp(a, 0
                , 255)).getRGB();
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BaseBlock block = getExtent().getFullBlock(position);
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block.getBlockType());
        int newColor = util.averageColor(currentColor, color);
        return util.getNearestBlock(newColor).getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType blockType = get.getBlock(extent).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(blockType);
        if (currentColor == 0) {
            return false;
        }
        int newColor = util.averageColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock == blockType) {
            return false;
        }
        return set.setBlock(extent, newBlock.getDefaultState());
    }

}
