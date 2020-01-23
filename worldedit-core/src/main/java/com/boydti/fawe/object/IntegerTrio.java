package com.boydti.fawe.object;

public class IntegerTrio {
    public int x, y, z;

    public IntegerTrio(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public IntegerTrio(IntegerTrio node) {
        this.x = node.x;
        this.y = node.y;
        this.z = node.z;
    }

    public IntegerTrio() {
    }

    public final void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final void set(IntegerTrio node) {
        this.x = node.x;
        this.y = node.y;
        this.z = node.z;
    }

    @Override
    public final int hashCode() {
        return (x ^ (z << 12)) ^ (y << 24);
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object obj) {
        IntegerTrio other = (IntegerTrio) obj;
        return other.x == x && other.z == z && other.y == y;
    }
}
