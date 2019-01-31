package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;
import java.util.SplittableRandom;

public class SolidRandomOffsetPattern extends AbstractPattern {
    private final int dx, dy, dz;
    private final Pattern pattern;

    private transient int dx2, dy2, dz2;
    private transient MutableBlockVector mutable;
    private transient boolean[] solid;
    private SplittableRandom r;


    public SolidRandomOffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        init();
    }

    private void init() {
        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        solid = SolidBlockMask.getTypes();
        this.r = new SplittableRandom();
        this.mutable = new MutableBlockVector();
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        mutable.mutX((position.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((position.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((position.getZ() + r.nextInt(dz2) - dz));
        BlockStateHolder block = pattern.apply(mutable.toBlockVector3());
        if (solid[block.getInternalBlockTypeId()]) {
            return block;
        } else {
            return pattern.apply(position);
        }
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        mutable.mutX((get.getX() + r.nextInt(dx2) - dx));
        mutable.mutY((get.getY() + r.nextInt(dy2) - dy));
        mutable.mutZ((get.getZ() + r.nextInt(dz2) - dz));
        BlockStateHolder block = pattern.apply(mutable.toBlockVector3());
        if (solid[block.getInternalBlockTypeId()]) {
            return pattern.apply(extent, set, mutable.toBlockVector3());
        } else {
            return pattern.apply(extent, set, get);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }
}