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
        mutable.mutX((position.x() + r.nextInt(dx2) - dx));
        mutable.mutY((position.y() + r.nextInt(dy2) - dy));
        mutable.mutZ((position.z() + r.nextInt(dz2) - dz));
        if (mutable.y() < minY || mutable.y() > maxY) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        return pattern.applyBlock(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX((set.x() + r.nextInt(dx2) - dx));
        mutable.mutY((set.y() + r.nextInt(dy2) - dy));
        mutable.mutZ((set.z() + r.nextInt(dz2) - dz));
        if (mutable.y() < extent.getMinY() || mutable.y() > extent.getMaxY()) {
            return false;
        }
        return pattern.apply(extent, get, mutable);
    }

    @Override
    public BlockVector3 size() {
        return BlockVector3.at(dx2, dy2, dz2);
    }

    @Override
    public Pattern fork() {
        return new RandomOffsetPattern(this.pattern.fork(), this.dx, this.dy, this.dz, this.minY, this.maxY);
    }

}
