package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class Linear3DBlockPattern extends AbstractPattern {

    private final Pattern[] patternsArray;

    public Linear3DBlockPattern(Pattern[] patterns) {
        this.patternsArray = patterns;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        int index = (position.getBlockX() + position.getBlockY() + position.getBlockZ()) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        int index = (get.getBlockX() + get.getBlockY() + get.getBlockZ()) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(extent, set, get);
    }
}
