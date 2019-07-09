package com.boydti.fawe.object.pattern;

import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;

import java.awt.Color;

public class AverageColorPattern extends AbstractExtentPattern {
    private transient TextureHolder holder;
    private final int color;

    public AverageColorPattern(Extent extent, int color, TextureHolder util) {
        super(extent);
        this.holder = util;
        this.color = new Color(color).getRGB();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
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
        if (currentColor == 0) return false;
        int newColor = util.averageColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock == blockType) return false;
        return set.setBlock(extent, newBlock.getDefaultState());
    }
}