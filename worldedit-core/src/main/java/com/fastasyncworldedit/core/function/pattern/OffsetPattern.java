package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class OffsetPattern extends AbstractPattern {

    private final int dx;
    private final int dy;
    private final int dz;
    private final int minY;
    private final int maxY;
    private final transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private final Pattern pattern;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param pattern pattern to apply
     * @param dx      offset x
     * @param dy      offset y
     * @param dz      offset z
     * @param minY    min applicable y (inclusive
     * @param maxY    max applicable y (inclusive
     */
    public OffsetPattern(Pattern pattern, int dx, int dy, int dz, int minY, int maxY) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.minY = minY;
        this.maxY = maxY;
        this.pattern = pattern;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        mutable.mutX(position.getX() + dx);
        mutable.mutY(position.getY() + dy);
        mutable.mutZ(position.getZ() + dz);
        return pattern.applyBlock(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX(get.getX() + dx);
        mutable.mutY(get.getY() + dy);
        if (mutable.getY() < minY || mutable.getY() > maxY) {
            return false;
        }
        mutable.mutZ(get.getZ() + dz);
        return pattern.apply(extent, get, mutable);
    }

}
