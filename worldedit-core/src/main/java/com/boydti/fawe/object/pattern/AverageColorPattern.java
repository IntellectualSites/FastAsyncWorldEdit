package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.Color;
import java.io.IOException;

public class AverageColorPattern extends AbstractExtentPattern {
    private transient TextureHolder holder;
    private final int color;

    public AverageColorPattern(Extent extent, int color, TextureHolder util) {
        super(extent);
        this.holder = util;
        this.color = new Color(color).getRGB();
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        BlockStateHolder block = getExtent().getBlock(position);
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block.getBlockType());
        int newColor = util.averageColor(currentColor, color);
        return util.getNearestBlock(newColor).getDefaultState();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition) throws WorldEditException {
        BlockType blockType = extent.getBlockType(getPosition);
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(blockType);
        if (currentColor == 0) return false;
        int newColor = util.averageColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock == blockType) return false;
        return extent.setBlock(setPosition, newBlock.getDefaultState());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        holder = Fawe.get().getCachedTextureUtil(true, 0, 100);
    }
}