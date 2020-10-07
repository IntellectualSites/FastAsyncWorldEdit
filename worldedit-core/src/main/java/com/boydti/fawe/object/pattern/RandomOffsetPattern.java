package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.SplittableRandom;

public class RandomOffsetPattern extends AbstractPattern {
    private final int dx;
    private final int dy;
    private final int dz;
    private final Pattern pattern;

    private transient int dx2;
    private transient int dy2;
    private transient int dz2;
    private transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private transient SplittableRandom r;

    public RandomOffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        this.r = new SplittableRandom();

    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        mutable.mutX((position.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((position.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((position.getZ() + r.nextInt(dz2) - dz));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX((set.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((set.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((set.getZ() + r.nextInt(dz2) - dz));
        return pattern.apply(extent, get, mutable);
    }
}
