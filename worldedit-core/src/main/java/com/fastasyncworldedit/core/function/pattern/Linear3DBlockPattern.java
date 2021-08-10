package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class Linear3DBlockPattern extends AbstractPattern {

    private final Pattern[] patternsArray;
    private final int xScale;
    private final int yScale;
    private final int zScale;

    public Linear3DBlockPattern(Pattern[] patterns, int xScale, int yScale, int zScale) {
        this.patternsArray = patterns;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        int index = (position.getBlockX() / this.xScale
                + position.getBlockY() / this.yScale + position.getBlockZ() / this.zScale) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        int index = (get.getBlockX() / this.xScale
                + get.getBlockY() / this.yScale + get.getBlockZ() / this.zScale) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(extent, get, set);
    }

}
