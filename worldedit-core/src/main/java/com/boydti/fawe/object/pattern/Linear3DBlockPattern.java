package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

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
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        int index = (get.getBlockX() + get.getBlockY() + get.getBlockZ()) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(extent, get, set);
    }
}
