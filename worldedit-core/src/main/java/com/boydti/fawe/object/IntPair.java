package com.boydti.fawe.object;

public final class IntPair {
    public int x;
    public int z;

    public IntPair(final int x, final int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public int hashCode() {
        return (x << 16) | (z & 0xFFFF);
    }

    @Override
    public String toString() {
        return x + "," + z;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (this.getClass() != obj.getClass())) {
            return false;
        }
        final IntPair other = (IntPair) obj;
        return (this.x == other.x) && (this.z == other.z);
    }
}
