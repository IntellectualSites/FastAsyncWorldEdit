package com.sk89q.worldedit;

import java.io.IOException;
import java.io.Serializable;

public final class MutableBlockVector2D extends BlockVector2D implements Serializable {
    private static ThreadLocal<MutableBlockVector2D> MUTABLE_CACHE = new ThreadLocal<MutableBlockVector2D>() {
        @Override
        protected MutableBlockVector2D initialValue() {
            return new MutableBlockVector2D();
        }
    };

    public static MutableBlockVector2D get(int x, int z) {
        return MUTABLE_CACHE.get().setComponents(x, z);
    }

    private transient int x, z;

    public MutableBlockVector2D() {
        super(0, 0);
        this.x = 0;
        this.z = 0;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public int getBlockX() {
        return x;
    }

    @Override
    public int getBlockZ() {
        return z;
    }

    public MutableBlockVector2D setComponents(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    public MutableBlockVector2D setComponents(double x, double z) {
        return setComponents((int) x, (int) z);
    }

    public final void mutX(int x) {
        this.x = x;
    }

    public void mutZ(int z) {
        this.z = z;
    }

    public final void mutX(double x) {
        this.x = (int) x;
    }

    public void mutZ(double z) {
        this.z = (int) z;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeInt(x);
        stream.writeInt(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readInt();
        this.z = stream.readInt();
    }

    public MutableBlockVector2D nextPosition() {
        int absX = Math.abs(x);
        int absY = Math.abs(z);
        if (absX > absY) {
            if (x > 0) {
                return setComponents(x, z + 1);
            } else {
                return setComponents(x, z - 1);
            }
        } else if (absY > absX) {
            if (z > 0) {
                return setComponents(x - 1, z);
            } else {
                return setComponents(x + 1, z);
            }
        } else {
            if (x == z && x > 0) {
                return setComponents(x, z + 1);
            }
            if (x == absX) {
                return setComponents(x, z + 1);
            }
            if (z == absY) {
                return setComponents(x, z - 1);
            }
            return setComponents(x + 1, z);
        }
    }
}
