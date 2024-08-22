package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.fastasyncworldedit.core.util.MathMan;

public final class BitArrayUnstretched {

    private final long[] data;
    private final int bitsPerEntry;
    private final int maxSeqLocIndex;
    private final int emptyBitCount;
    private final long mask;
    private final int longLen;

    public BitArrayUnstretched(int bitsPerEntry, int arraySize, long[] buffer) {
        this.bitsPerEntry = bitsPerEntry;
        this.mask = (1L << bitsPerEntry) - 1L;
        this.emptyBitCount = 64 % bitsPerEntry;
        this.maxSeqLocIndex = 64 - (bitsPerEntry + emptyBitCount);
        final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntry);
        this.longLen = MathMan.ceilZero((float) arraySize / blocksPerLong);
        if (buffer.length < longLen) {
            this.data = new long[longLen];
        } else {
            this.data = buffer;
        }
    }

    public BitArrayUnstretched(int bitsPerEntry, int arraySize) {
        this.bitsPerEntry = bitsPerEntry;
        this.mask = (1L << bitsPerEntry) - 1L;
        this.emptyBitCount = 64 % bitsPerEntry;
        this.maxSeqLocIndex = 64 - bitsPerEntry;
        final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntry);
        this.longLen = MathMan.ceilZero((float) arraySize / blocksPerLong);
        this.data = new long[longLen];
    }

    public long[] getData() {
        return data;
    }

    public void set(int index, int value) {
        if (longLen == 0) {
            return;
        }
        int bitIndexStart = index * bitsPerEntry + MathMan.floorZero((double) index / longLen) * emptyBitCount;
        int longIndexStart = bitIndexStart >> 6;
        int localBitIndexStart = bitIndexStart & 63;
        this.data[longIndexStart] = this.data[longIndexStart] & ~(mask << localBitIndexStart) | (long) value << localBitIndexStart;
    }

    public int get(int index) {
        if (longLen == 0) {
            return 0;
        }
        int bitIndexStart = index * bitsPerEntry + MathMan.floorZero((double) index / longLen) * emptyBitCount;

        int longIndexStart = bitIndexStart >> 6;

        int localBitIndexStart = bitIndexStart & 63;
        return (int) (this.data[longIndexStart] >>> localBitIndexStart & mask);
    }

    public int getLength() {
        return longLen;
    }

    public void fromRaw(int[] arr) {
        final long[] data = this.data;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        long l = 0;
        for (int i = 0; i < longLen; i++) {
            int lastVal;
            for (; localStart <= maxSeqLocIndex && arrI < arr.length; localStart += bitsPerEntry) {
                lastVal = arr[arrI++];
                l |= ((long) lastVal << localStart);
            }
            localStart = 0;
            data[i] = l;
            l = 0;
        }
    }

    public int[] toRaw() {
        return toRaw(new int[4096]);
    }

    public int[] toRaw(int[] buffer) {
        final long[] data = this.data;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        for (int i = 0; i < longLen; i++) {
            long l = data[i];
            char lastVal;
            for (; localStart <= maxSeqLocIndex && arrI < buffer.length; localStart += bitsPerEntry) {
                lastVal = (char) (l >>> localStart & this.mask);
                buffer[arrI++] = lastVal;
            }
            localStart = 0;
        }
        return buffer;
    }

    public DataArray toRaw(DataArray buffer) {
        final long[] data = this.data;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        for (int i = 0; i < longLen; i++) {
            long l = data[i];
            char lastVal;
            for (; localStart <= maxSeqLocIndex && arrI < DataArray.CHUNK_SECTION_SIZE; localStart += bitsPerEntry) {
                lastVal = (char) (l >>> localStart & this.mask);
                buffer.setAt(arrI++, lastVal);
            }
            localStart = 0;
        }
        return buffer;
    }

}
