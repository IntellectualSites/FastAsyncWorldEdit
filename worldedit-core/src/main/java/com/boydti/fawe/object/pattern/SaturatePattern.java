package com.boydti.fawe.object.pattern;

import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import java.awt.Color;

public class SaturatePattern extends AbstractPattern {
    private final TextureHolder holder;
    private final int color;
    private final Extent extent;


    public SaturatePattern(Extent extent, int color, TextureHolder texture) {
        this.extent = extent;
        this.holder = texture;
        this.color = new Color(color).getRGB();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BlockType block = extent.getBlock(position).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block);
        int newColor = util.multiplyColor(currentColor, color);
        return util.getNearestBlock(newColor).getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType block = get.getBlock(extent).getBlockType();
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block);
        if (currentColor == 0) return false;
        int newColor = util.multiplyColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock.equals(block)) return false;
        return set.setBlock(extent, newBlock.getDefaultState());
    }
}
