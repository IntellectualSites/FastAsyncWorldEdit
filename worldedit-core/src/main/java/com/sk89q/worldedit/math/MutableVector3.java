package com.sk89q.worldedit.math;

public class MutableVector3 extends Vector3 {

    public MutableVector3() {
    }

    public MutableVector3(double x, double y, double z) {
        super(x, y, z);
    }

    public MutableVector3(float x, float y, float z) {
        super(x, y, z);
    }

    public MutableVector3(Vector3 other) {
        super(other);
    }

    @Override
    public MutableVector3 setComponents(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
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

    public double getY() {
        return y;
    }
}
