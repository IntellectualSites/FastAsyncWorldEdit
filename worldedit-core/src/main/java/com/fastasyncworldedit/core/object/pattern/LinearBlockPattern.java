package com.fastasyncworldedit.core.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class LinearBlockPattern extends AbstractPattern implements ResettablePattern {

    private final Pattern[] patternsArray;
    private transient int index;

    public LinearBlockPattern(Pattern[] patterns) {
        this.patternsArray = patterns;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        if (index == patternsArray.length) {
            index = 0;
        }
        return patternsArray[index++].applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (index == patternsArray.length) {
            index = 0;
        }
        return patternsArray[index++].apply(extent, get, set);
    }

    @Override
    public void reset() {
        index = 0;
    }
}
