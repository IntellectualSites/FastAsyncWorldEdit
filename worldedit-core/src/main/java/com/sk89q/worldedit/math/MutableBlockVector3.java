package com.sk89q.worldedit.math;

public class MutableBlockVector3 extends BlockVector3 {

    private static ThreadLocal<MutableBlockVector3> MUTABLE_CACHE = new ThreadLocal<MutableBlockVector3>() {
        @Override
        protected MutableBlockVector3 initialValue() {
            return new MutableBlockVector3();
        }
    };

    public static MutableBlockVector3 get(int x, int y, int z) {
        return MUTABLE_CACHE.get().setComponents(x, y, z);
    }

    public MutableBlockVector3() {}

    public MutableBlockVector3(BlockVector3 other) {
        super(other.getX(), other.getY(), other.getZ());
    }

    public MutableBlockVector3 setComponents(BlockVector3 other) {
        return setComponents(other.getBlockX(), other.getBlockY(), other.getBlockZ());
    }

    public MutableBlockVector3(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public MutableBlockVector3 setComponents(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
