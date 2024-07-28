package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.math.Vector3;

public class MutableVector3 extends Vector3 {

    private double x;
    private double y;
    private double z;

    public MutableVector3() {
    }

    public MutableVector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVector3(float x, float y, float z) {
        this(x, y, (double) z);
    }

    public MutableVector3(Vector3 other) {
        this(other.x(), other.y(), other.z());
    }

    public static MutableVector3 get(int x, int y, int z) {
        return FaweCache.INSTANCE.MUTABLE_VECTOR3.get().setComponents(x, y, z);
    }

    public static MutableVector3 get(double x, double y, double z) {
        return FaweCache.INSTANCE.MUTABLE_VECTOR3.get().setComponents(x, y, z);
    }

    @Override
    public MutableVector3 setComponents(Vector3 other) {
        this.x = other.x();
        this.y = other.y();
        this.z = other.z();
        return this;
    }

    @Override
    public MutableVector3 setComponents(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public MutableVector3 setComponents(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public MutableVector3 mutX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableVector3 mutX(double x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableVector3 mutY(int y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableVector3 mutY(double y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableVector3 mutZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableVector3 mutZ(double z) {
        this.z = z;
        return this;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

}
