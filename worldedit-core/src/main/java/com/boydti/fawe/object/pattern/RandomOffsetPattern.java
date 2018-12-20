package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;
import java.util.SplittableRandom;

public class RandomOffsetPattern extends AbstractPattern {
    private final int dx, dy, dz;
    private final Pattern pattern;

    private transient int dx2, dy2, dz2;
    private transient MutableBlockVector mutable = new MutableBlockVector();
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
    public BlockStateHolder apply(Vector position) {
        mutable.mutX((position.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((position.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((position.getZ() + r.nextInt(dz2) - dz));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        mutable.mutX((get.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((get.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((get.getZ() + r.nextInt(dz2) - dz));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        this.r = new SplittableRandom();
        this.mutable = new MutableBlockVector();
    }
}