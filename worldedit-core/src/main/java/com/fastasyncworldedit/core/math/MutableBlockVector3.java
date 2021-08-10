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
        return FaweCache.IMP.MUTABLE_BLOCKVECTOR3.get().setComponents(x, y, z);
    }

    public MutableBlockVector3() {
    }

    public MutableBlockVector3(BlockVector3 other) {
        this(other.getX(), other.getY(), other.getZ());
    }

    public MutableBlockVector3 setComponents(BlockVector3 other) {
        return setComponents(other.getBlockX(), other.getBlockY(), other.getBlockZ());
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
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public final int getZ() {
        return z;
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
