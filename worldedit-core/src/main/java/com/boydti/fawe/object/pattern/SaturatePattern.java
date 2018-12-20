package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureHolder;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.awt.Color;
import java.io.IOException;

public class SaturatePattern extends AbstractPattern {
    private transient TextureHolder holder;
    private final int color;
    private final Extent extent;


    public SaturatePattern(Extent extent, int color, TextureHolder texture) {
        this.extent = extent;
        this.holder = texture;
        this.color = new Color(color).getRGB();
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        BlockType block = extent.getBlockType(position);
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block);
        int newColor = util.multiplyColor(currentColor, color);
        return util.getNearestBlock(newColor).getDefaultState();
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        BlockType block = extent.getBlockType(getPosition);
        TextureUtil util = holder.getTextureUtil();
        int currentColor = util.getColor(block);
        if (currentColor == 0) return false;
        int newColor = util.multiplyColor(currentColor, color);
        BlockType newBlock = util.getNearestBlock(newColor);
        if (newBlock.equals(block)) return false;
        return extent.setBlock(setPosition, newBlock.getDefaultState());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        holder = Fawe.get().getCachedTextureUtil(true, 0, 100);
    }
}