package com.fastasyncworldedit.core.math;

public record IntPair(int x, int z) {

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
