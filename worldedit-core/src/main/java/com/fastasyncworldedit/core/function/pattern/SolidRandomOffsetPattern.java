package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.SplittableRandom;

public class SolidRandomOffsetPattern extends AbstractPattern implements StatefulPattern {

    private final int dx;
    private final int dy;
    private final int dz;
    private final int minY;
    private final int maxY;
    private final Pattern pattern;

    private final int dx2;
    private final int dy2;
    private final int dz2;
    private final MutableBlockVector3 mutable;
    private final SplittableRandom r;

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
    public SolidRandomOffsetPattern(Pattern pattern, int dx, int dy, int dz, int minY, int maxY) {
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.minY = minY;
        this.maxY = maxY;

        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        this.r = new SplittableRandom();
        this.mutable = new MutableBlockVector3();
    }

    public static boolean[] getTypes() {
        boolean[] types = new boolean[BlockTypes.size()];
        for (BlockType type : BlockTypesCache.values) {
            types[type.getInternalId()] = type.getMaterial().isSolid();
        }
        return types;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        mutable.mutX(position.getX() + r.nextInt(dx2) - dx);
        mutable.mutY(position.getY() + r.nextInt(dy2) - dy);
        mutable.mutZ(position.getZ() + r.nextInt(dz2) - dz);
        if (mutable.getY() < minY || mutable.getY() > maxY) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        if (mutable.getY() < minY || mutable.getY() > maxY) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        BaseBlock block = pattern.applyBlock(mutable);
        if (block.getMaterial().isSolid()) {
            return block;
        }
        return pattern.applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX(set.getX() + r.nextInt(dx2) - dx);
        mutable.mutY(set.getY() + r.nextInt(dy2) - dy);
        mutable.mutZ(set.getZ() + r.nextInt(dz2) - dz);
        if (mutable.getY() < extent.getMinY() || mutable.getY() > extent.getMaxY()) {
            return false;
        }
        BaseBlock block = pattern.applyBlock(mutable);
        if (block.getMaterial().isSolid()) {
            return pattern.apply(extent, get, mutable);
        }
        return pattern.apply(extent, get, set);
    }

    @Override
    public StatefulPattern fork() {
        return new SolidRandomOffsetPattern(this.pattern.fork(), this.dx, this.dy, this.dz, this.minY, this.maxY);
    }

}
