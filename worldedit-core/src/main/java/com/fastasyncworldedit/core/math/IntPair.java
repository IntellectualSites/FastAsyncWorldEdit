package com.fastasyncworldedit.core.math;

public record IntPair(int x, int z) {

    @Override
    public int hashCode() {
        int i = 1664525 * x + 1013904223;
        int j = 1664525 * (z ^ -559038737) + 1013904223;
        return i ^ j;
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
