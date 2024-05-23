package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.math.BlockVector3;

public class MutableBlockVector3 extends BlockVector3 {

    public static MutableBlockVector3 at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static MutableBlockVector3 at(int x, int y, int z) {
        return new MutableBlockVector3(x, y, z);
    }

    public static MutableBlockVector3 get(int x, int y, int z) {
        return FaweCache.INSTANCE.MUTABLE_BLOCKVECTOR3.get().setComponents(x, y, z);
    }

    public MutableBlockVector3() {
    }

    public MutableBlockVector3(BlockVector3 other) {
        this(other.x(), other.y(), other.z());
    }

    public MutableBlockVector3 setComponents(BlockVector3 other) {
        return setComponents(other.x(), other.y(), other.z());
    }

    private int x;
    private int y;
    private int z;

    public MutableBlockVector3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public MutableBlockVector3 setComponents(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public final int x() {
        return x;
    }

    @Override
    public final int y() {
        return y;
    }

    @Override
    public final int z() {
        return z;
    }

    @Override
    public BlockVector3 getMinimum(BlockVector3 v2) {
        this.x = Math.min(v2.x(), x);
        this.y = Math.min(v2.y(), y);
        this.z = Math.min(v2.z(), z);
        return this;
    }

    @Override
    public BlockVector3 getMaximum(BlockVector3 v2) {
        this.x = Math.max(v2.x(), x);
        this.y = Math.max(v2.y(), y);
        this.z = Math.max(v2.z(), z);
        return this;
    }

    @Override
    public MutableBlockVector3 mutX(double x) {
        this.x = (int) x;
        return this;
    }

    @Override
    public MutableBlockVector3 mutY(double y) {
        this.y = (int) y;
        return this;
    }

    @Override
    public MutableBlockVector3 mutZ(double z) {
        this.z = (int) z;
        return this;
    }

    @Override
    public final MutableBlockVector3 mutX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public final MutableBlockVector3 mutY(int y) {
        this.y = y;
        return this;
    }

    @Override
    public final MutableBlockVector3 mutZ(int z) {
        this.z = z;
        return this;
    }

}
