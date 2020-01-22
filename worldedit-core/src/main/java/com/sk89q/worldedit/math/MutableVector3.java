package com.sk89q.worldedit.math;

import com.boydti.fawe.FaweCache;

public class MutableVector3 extends Vector3 {

    private double x, y, z;

    public MutableVector3() {
    }

    public static MutableVector3 get(int x, int y, int z) {
        return FaweCache.INSTANCE.getMutableVector3().get().setComponents(x, y, z);
    }

    public static MutableVector3 get(double x, double y, double z) {
        return FaweCache.INSTANCE.getMutableVector3().get().setComponents(x, y, z);
    }

    public MutableVector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVector3(float x, float y, float z) {
        this((double) x, (double) y, (double) z);
    }

    public MutableVector3(Vector3 other) {
        this(other.getX(), other.getY(), other.getZ());
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public MutableVector3 setComponents(Vector3 other) {
        this.x = other.getX();
        this.y = other.getY();
        this.z = other.getZ();
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
    public MutableVector3 setComponents(int x, int y, int z) {
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
    public MutableVector3 mutZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableVector3 mutX(double x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableVector3 mutZ(double z) {
        this.z = z;
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

}
