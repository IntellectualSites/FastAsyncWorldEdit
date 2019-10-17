package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import java.util.SplittableRandom;

public class RandomOffsetPattern extends OffsetPattern {
    protected transient int dx2;
    protected transient int dy2;
    protected transient int dz2;
    protected transient SplittableRandom r;

    public RandomOffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        super(pattern, dx, dy, dz);
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
