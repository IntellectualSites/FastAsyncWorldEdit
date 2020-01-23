package com.boydti.fawe.object;

import com.boydti.fawe.util.MathMan;

public class BytePair {
    public short pair;

    public BytePair(final byte x, final byte z) {
        this.pair = MathMan.pairByte(x, z);
    }

    public int get0x() {
        return MathMan.unpair16x((byte) get0());
    }

    public int get0y() {
        return MathMan.unpair16y((byte) get0());
    }

    public int get0() {
        return MathMan.unpairShortX(pair);
    }

    public int get1() {
        return MathMan.unpairShortY(pair);
    }

    @Override
    public int hashCode() {
        return pair;
    }

    @Override
    public String toString() {
        return pair + "";
    }

    @Override
    public boolean equals(final Object obj) {
        return obj.hashCode() == pair;
    }
}
