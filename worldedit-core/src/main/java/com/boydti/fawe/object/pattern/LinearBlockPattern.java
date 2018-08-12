package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class LinearBlockPattern extends AbstractPattern implements ResettablePattern {

    private final Pattern[] patternsArray;
    private transient int index;

    public LinearBlockPattern(Pattern[] patterns) {
        this.patternsArray = patterns;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        if (index == patternsArray.length) {
            index = 0;
        }
        return patternsArray[index++].apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        if (index == patternsArray.length) {
            index = 0;
        }
        return patternsArray[index++].apply(extent, set, get);
    }

    @Override
    public void reset() {
        index = 0;
    }
}
