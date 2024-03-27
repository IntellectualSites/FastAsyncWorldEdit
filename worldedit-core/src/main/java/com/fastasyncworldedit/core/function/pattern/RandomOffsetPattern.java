package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.SplittableRandom;

public class RandomOffsetPattern extends AbstractPattern {

    private final int dx;
    private final int dy;
    private final int dz;
    private final int minY;
    private final int maxY;
    private final Pattern pattern;

    private final transient int dx2;
    private final transient int dy2;
    private final transient int dz2;
    private final transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private final transient SplittableRandom r;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param pattern pattern to apply
     * @param dx      offset x
     * @param dy      offset y
     * @param dz      offset z
     * @param minY    min applicable y (inclusive)
     * @param maxY    max applicable y (inclusive)
     */
    public RandomOffsetPattern(Pattern pattern, int dx, int dy, int dz, int minY, int maxY) {
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        this.r = new SplittableRandom();
        this.minY = minY;
        this.maxY = maxY;

    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        mutable.mutX((position.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((position.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((position.getZ() + r.nextInt(dz2) - dz));
        if (mutable.getY() < minY || mutable.getY() > maxY) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        return pattern.applyBlock(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX((set.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((set.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((set.getZ() + r.nextInt(dz2) - dz));
        if (mutable.getY() < extent.getMinY() || mutable.getY() > extent.getMaxY()) {
            return false;
        }
        return pattern.apply(extent, get, mutable);
    }

    @Override
    public Pattern fork() {
        return new RandomOffsetPattern(this.pattern.fork(), this.dx, this.dy, this.dz, this.minY, this.maxY);
    }

}
