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
    public MutableBlockVector3 getMinimum(BlockVector3 v2) {
        this.x = Math.min(v2.x(), x);
        this.y = Math.min(v2.y(), y);
        this.z = Math.min(v2.z(), z);
        return this;
    }

    @Override
    public MutableBlockVector3 getMaximum(BlockVector3 v2) {
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

    @Override
    public final MutableBlockVector3 withX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public final MutableBlockVector3 withY(int y) {
        this.y = y;
        return this;
    }

    @Override
    public final MutableBlockVector3 withZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableBlockVector3 add(BlockVector3 other) {
        return add(other.x(), other.y(), other.z());
    }

    @Override
    public MutableBlockVector3 add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public MutableBlockVector3 add(BlockVector3... others) {
        for (BlockVector3 other : others) {
            this.x += other.x();
            this.y += other.y();
            this.z += other.z();
        }
        return this;
    }

    @Override
    public MutableBlockVector3 subtract(BlockVector3 other) {
        return subtract(other.x(), other.y(), other.z());
    }

    @Override
    public MutableBlockVector3 subtract(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    @Override
    public MutableBlockVector3 subtract(BlockVector3... others) {
        for (BlockVector3 other : others) {
            this.x -= other.x();
            this.y -= other.y();
            this.z -= other.z();
        }
        return this;
    }

    @Override
    public MutableBlockVector3 multiply(BlockVector3 other) {
        return multiply(other.x(), other.y(), other.z());
    }

    @Override
    public MutableBlockVector3 multiply(int x, int y, int z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    @Override
    public MutableBlockVector3 multiply(BlockVector3... others) {
        for (BlockVector3 other : others) {
            this.x *= other.x();
            this.y *= other.y();
            this.z *= other.z();
        }
        return this;
    }

    @Override
    public MutableBlockVector3 multiply(int n) {
        return multiply(n, n, n);
    }

    @Override
    public MutableBlockVector3 divide(BlockVector3 other) {
        return divide(other.x(), other.y(), other.z());
    }

    @Override
    public MutableBlockVector3 divide(int x, int y, int z) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        return this;
    }

    @Override
    public MutableBlockVector3 divide(int n) {
        return divide(n, n, n);
    }

    @Override
    public MutableBlockVector3 shr(int x, int y, int z) {
        this.x = this.x >> x;
        this.y = this.y >> y;
        this.z = this.z >> z;
        return this;
    }

    @Override
    public MutableBlockVector3 shr(int n) {
        return shr(n, n, n);
    }

    @Override
    public MutableBlockVector3 shl(int x, int y, int z) {
        this.x = this.x >> x;
        this.y = this.y >> y;
        this.z = this.z >> z;
        return this;
    }

    @Override
    public MutableBlockVector3 shl(int n) {
        return shl(n, n, n);
    }

}
