package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class NoYPattern extends AbstractPattern {

    private final Pattern pattern;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    /**
     * Create a new {@link Pattern} instance
     *
     * @param pattern pattern to apply
     */
    public NoYPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 pos) {
        mutable.mutX(pos.getX());
        mutable.mutZ(pos.getZ());
        return pattern.applyBlock(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX(get.getX());
        mutable.mutZ(get.getZ());
        return pattern.apply(extent, mutable, set);
    }

    @Override
    public Pattern fork() {
        return new NoYPattern(this.pattern.fork());
    }

}
