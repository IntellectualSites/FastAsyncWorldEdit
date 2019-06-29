package com.sk89q.worldedit.math;

public class MutableBlockVector2 extends BlockVector2 {

    private static ThreadLocal<MutableBlockVector2> MUTABLE_CACHE = ThreadLocal.withInitial(() -> new MutableBlockVector2());

    public static MutableBlockVector2 get(int x, int z) {
        return MUTABLE_CACHE.get().setComponents(x, z);
    }

    public MutableBlockVector2() {}

    public MutableBlockVector2(int x, int z) {
        super(x, z);
    }

    @Override
    public MutableBlockVector2 setComponents(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    @Override
    public MutableBlockVector2 mutX(double x) {
        this.x = (int) x;
        return this;
    }

    @Override
    public MutableBlockVector2 mutZ(double z) {
        this.z = (int) z;
        return this;
    }

    @Override
    public MutableBlockVector2 mutX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableBlockVector2 mutZ(int z) {
        this.z = z;
        return this;
    }
}
