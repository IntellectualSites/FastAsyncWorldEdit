package com.sk89q.worldedit.math;

import java.io.Serializable;

public class MutableVector2 extends Vector2 {
    public MutableVector2() {}

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public MutableVector2(double x, double z) {
        super(x, z);
    }

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public MutableVector2(float x, float z) {
        super(x, z);
    }

    /**
     * Copy another vector.
     *
     * @param other the other vector
     */
    public MutableVector2(Vector2 other) {
        super(other);
    }

    @Override
    public MutableVector2 setComponents(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    @Override
    public MutableVector2 setComponents(double x, double z) {
        this.x = x;
        this.z = z;
        return this;
    }
    @Override
    public MutableVector2 mutX(int x) {
        this.x = x;
        return this;
    }
    @Override
    public MutableVector2 mutZ(int z) {
        this.z = z;
        return this;
    }
    @Override
    public MutableVector2 mutX(double x) {
        this.x = x;
        return this;
    }
    @Override
    public MutableVector2 mutZ(double z) {
        this.z = z;
        return this;
    }
}
