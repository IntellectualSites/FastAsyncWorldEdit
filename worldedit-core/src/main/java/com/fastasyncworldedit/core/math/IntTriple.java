package com.fastasyncworldedit.core.math;

public record IntTriple(int x, int y, int z) {

    @Override
    public int hashCode() {
        return x ^ (z << 12) ^ (y << 24);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntTriple(int x1, int y1, int z1)) {
            return x1 == x && z1 == z && y1 == y;
        }
        return false;
    }

}
