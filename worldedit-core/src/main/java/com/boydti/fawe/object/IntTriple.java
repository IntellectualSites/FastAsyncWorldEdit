package com.boydti.fawe.object;

public final class IntTriple {

    public int x;
    public int y;
    public int z;

    public IntTriple(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public final int hashCode() {
        return x ^ (z << 12) ^ (y << 24);
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
        if (obj instanceof IntTriple) {
            IntTriple other = (IntTriple) obj;
            return other.x == x && other.z == z && other.y == y;
        }
        return false;
    }
}
