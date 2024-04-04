package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.Arrays;

import static java.lang.Math.floorDiv;

/**
 * @deprecated replaced by {@link com.sk89q.worldedit.function.pattern.RandomPattern}
 * combined with {@link com.fastasyncworldedit.core.math.random.Linear2DRandom}.
 */
@Deprecated(forRemoval = true, since = "2.9.2")
public class Linear2DBlockPattern extends AbstractPattern {

    private final Pattern[] patternsArray;
    private final int xScale;
    private final int zScale;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param patterns array of patterns to linearly choose from based on x/z coordinates
     * @param xScale   x-axis scale
     * @param zScale   z-axis scale
     */
    public Linear2DBlockPattern(Pattern[] patterns, int xScale, int zScale) {
        this.patternsArray = patterns;
        this.xScale = xScale;
        this.zScale = zScale;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        int index = (position.getBlockX() / this.xScale + position.getBlockZ() / this.zScale) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        int index = (floorDiv(get.getBlockX(), this.xScale)
                + floorDiv(get.getBlockZ(), this.zScale)) % patternsArray.length;
        if (index < 0) {
            index += patternsArray.length;
        }
        return patternsArray[index].apply(extent, get, set);
    }

    @Override
    public Pattern fork() {
        final Pattern[] forked = Arrays.stream(this.patternsArray).map(Pattern::fork).toArray(Pattern[]::new);
        return new Linear2DBlockPattern(forked, this.xScale, this.zScale);
    }

}
