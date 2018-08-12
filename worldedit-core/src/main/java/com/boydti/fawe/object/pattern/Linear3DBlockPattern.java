package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class Linear3DBlockPattern extends AbstractPattern {

    private final Pattern[] patternsArray;

    public Linear3DBlockPattern(Pattern[] patterns) {
        this.patternsArray = patterns;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        int index = (position.getBlockX() + position.getBlockY() + position.getBlockZ()) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        int index = (get.getBlockX() + get.getBlockY() + get.getBlockZ()) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(extent, set, get);
    }
}
