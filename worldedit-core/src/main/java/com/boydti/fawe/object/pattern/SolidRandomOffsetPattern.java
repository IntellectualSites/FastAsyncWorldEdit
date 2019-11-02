package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.SplittableRandom;

public class SolidRandomOffsetPattern extends AbstractPattern {
    private final int dx, dy, dz;
    private final Pattern pattern;

    private final int dx2, dy2, dz2;
    private final MutableBlockVector3 mutable;
    private SplittableRandom r;

    public static boolean[] getTypes() {
        boolean[] types = new boolean[BlockTypes.size()];
        for (BlockType type : BlockTypesCache.values) {
            types[type.getInternalId()] = type.getMaterial().isSolid();
        }
        return types;
    }

    public SolidRandomOffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;

        this.dx2 = dx * 2 + 1;
        this.dy2 = dy * 2 + 1;
        this.dz2 = dz * 2 + 1;
        this.r = new SplittableRandom();
        this.mutable = new MutableBlockVector3();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        mutable.mutX(position.getX() + r.nextInt(dx2) - dx);
        mutable.mutY(position.getY() + r.nextInt(dy2) - dy);
        mutable.mutZ(position.getZ() + r.nextInt(dz2) - dz);
        BaseBlock block = pattern.apply(mutable);
        if (block.getMaterial().isSolid()) {
            return block;
        }
        return pattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX(set.getX() + r.nextInt(dx2) - dx);
        mutable.mutY(set.getY() + r.nextInt(dy2) - dy);
        mutable.mutZ(set.getZ() + r.nextInt(dz2) - dz);
        BaseBlock block = pattern.apply(mutable);
        if (block.getMaterial().isSolid()) {
            return pattern.apply(extent, get, mutable);
        }
        return pattern.apply(extent, get, set);
    }
}
