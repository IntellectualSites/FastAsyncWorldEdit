package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

public class RelativePattern extends AbstractPattern implements ResettablePattern {

    private final Pattern pattern;
    private final int minY;
    private final int maxY;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private BlockVector3 origin;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param pattern pattern to apply
     * @param minY    min applicable y (inclusive
     * @param maxY    max applicable y (inclusive
     */
    public RelativePattern(Pattern pattern, int minY, int maxY) {
        this.pattern = pattern;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 pos) {
        if (origin == null) {
            origin = pos;
        }
        mutable.mutX(pos.x() - origin.x());
        mutable.mutY(pos.y() - origin.y());
        mutable.mutZ(pos.z() - origin.z());
        if (mutable.y() < minY || mutable.y() > maxY) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        return pattern.applyBlock(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (origin == null) {
            origin = set;
        }
        mutable.mutX(set.x() - origin.x());
        mutable.mutY(set.y() - origin.y());
        mutable.mutZ(set.z() - origin.z());
        if (mutable.y() < extent.getMinY() || mutable.y() > extent.getMaxY()) {
            return false;
        }
        return pattern.apply(extent, get, mutable);
    }

    @Override
    public void reset() {
        origin = null;
    }

    @Override
    public Pattern fork() {
        RelativePattern forked = new RelativePattern(this.pattern.fork(), this.minY, this.maxY);
        forked.origin = this.origin; // maintain origin for forks
        return forked;
    }

}
