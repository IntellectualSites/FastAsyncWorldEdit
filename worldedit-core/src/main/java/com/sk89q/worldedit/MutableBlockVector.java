package com.sk89q.worldedit;

import com.boydti.fawe.util.MathMan;
import java.io.IOException;
import java.io.Serializable;

public class MutableBlockVector extends BlockVector implements Serializable {
    private transient int x, y, z;

    private static ThreadLocal<MutableBlockVector> MUTABLE_CACHE = new ThreadLocal<MutableBlockVector>() {
        @Override
        protected MutableBlockVector initialValue() {
            return new MutableBlockVector();
        }
    };

    public static MutableBlockVector get(int x, int y, int z) {
        return MUTABLE_CACHE.get().setComponents(x, y, z);
    }

    public MutableBlockVector(Vector v) {
        this(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

    public MutableBlockVector(int x, int y, int z) {
        super(0, 0, 0);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableBlockVector() {
        super(0, 0, 0);
    }

    public MutableBlockVector setComponents(Vector other) {
        return setComponents(other.getBlockX(), other.getBlockY(), other.getBlockZ());
    }

    @Override
    public MutableBlockVector setComponents(double x, double y, double z) {
        return this.setComponents((int) x, (int) y, (int) z);
    }

    @Override
    public MutableBlockVector setComponents(int x, int y, int z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

    @Override
    public final void mutX(double x) {
        this.x = MathMan.roundInt(x);
    }

    @Override
    public final void mutY(double y) {
        this.y = MathMan.roundInt(y);
    }

    @Override
    public final void mutZ(double z) {
        this.z = MathMan.roundInt(z);
    }

    @Override
    public final void mutX(int x) {
        this.x = x;
    }

    @Override
    public final void mutY(int y) {
        this.y = y;
    }

    @Override
    public final void mutZ(int z) {
        this.z = z;
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

    @Override
    public int getBlockX() {
        return this.x;
    }

    @Override
    public int getBlockY() {
        return this.y;
    }

    @Override
    public int getBlockZ() {
        return this.z;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeInt(x);
        stream.writeByte((byte) y);
        stream.writeInt(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readInt();
        this.y = stream.readByte() & 0xFF;
        this.z = stream.readInt();
    }
}
