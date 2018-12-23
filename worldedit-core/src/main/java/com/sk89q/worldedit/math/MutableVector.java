package com.sk89q.worldedit.math;

import com.boydti.fawe.util.MathMan;
import java.io.IOException;
import java.io.Serializable;

public class MutableVector extends Vector3 implements Serializable {
    private transient double x, y, z;

    private static ThreadLocal<MutableVector> MUTABLE_CACHE = new ThreadLocal<MutableVector>() {
        @Override
        protected MutableVector initialValue() {
            return new MutableVector();
        }
    };

    public static MutableVector get(int x, int y, int z) {
        return MUTABLE_CACHE.get().setComponents(x, y, z);
    }

    public MutableVector(Vector3 v) {
        this(v.getX(), v.getY(), v.getZ());
    }

    public MutableVector(double x, double y, double z) {
        super(0, 0, 0);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVector() {
        super(0, 0, 0);
    }

    public MutableVector setComponents(BlockVector3 other) {
        return setComponents(other.getBlockX(), other.getBlockY(), other.getBlockZ());
    }

//    @Override
    public MutableVector setComponents(int x, int y, int z) {
        return this.setComponents((double) x, (double) y, (double) z);
    }

//    @Override
    public MutableVector setComponents(double x, double y, double z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

//    @Override
    public final void mutX(double x) {
        this.x = y;
    }

//    @Override
    public final void mutY(double y) {
        this.y =y;
    }

//    @Override
    public final void mutZ(double z) {
        this.z = y;
    }

//    @Override
    public final void mutX(int x) {
        this.x = (double)x;
    }

//    @Override
    public final void mutY(int y) {
        this.y = (double)y;
    }

//    @Override
    public final void mutZ(int z) {
        this.z = (double)z;
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

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeDouble(x);
        stream.writeByte((byte) y);
        stream.writeDouble(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readDouble();
        this.y = stream.readByte() & 0xFF;
        this.z = stream.readDouble();
    }
}