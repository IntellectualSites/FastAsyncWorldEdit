package com.sk89q.worldedit.math;

public class Vector3Impl extends Vector3 {
    private final double x,y,z;

    public Vector3Impl(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Impl(Vector3 other) {
        this(other.getX(), other.getY(), other.getZ());
    }

    @Override
    public final double getX() {
        return x;
    }

    @Override
    public final double getY() {
        return y;
    }

    @Override
    public final double getZ() {
        return z;
    }
}
