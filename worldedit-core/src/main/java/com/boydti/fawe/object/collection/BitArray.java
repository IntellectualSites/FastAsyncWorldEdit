package com.boydti.fawe.object.collection;

public final class BitArray {

    private final long[] data;
    private final int bitsPerEntry;
    private final int maxSeqLocIndex;
    private final long mask;
    private final int longLen;

    public BitArray(int bitsPerEntry, int arraySize, long[] buffer) {
        this.bitsPerEntry = bitsPerEntry;
        this.maxSeqLocIndex = 64 - bitsPerEntry;
        this.mask = (1L << bitsPerEntry) - 1L;
        this.longLen = (arraySize * bitsPerEntry) >> 6;
        if (buffer.length < longLen) {
            this.data = new long[longLen];
        } else {
            this.data = buffer;
        }
    }

    public BitArray(int bitsPerEntry, int arraySize) {
        this.bitsPerEntry = bitsPerEntry;
        this.maxSeqLocIndex = 64 - bitsPerEntry;
        this.mask = (1L << bitsPerEntry) - 1L;
        this.longLen = (arraySize * bitsPerEntry) >> 6;
        this.data = new long[longLen];
    }

    public long[] getData() {
        return data;
    }

    public final void set(int index, int value) {
        if (longLen == 0) return;
        int bitIndexStart = index * bitsPerEntry;
        int longIndexStart = bitIndexStart >> 6;
        int localBitIndexStart = bitIndexStart & 63;
        this.data[longIndexStart] = this.data[longIndexStart] & ~(mask << localBitIndexStart) | (long) value << localBitIndexStart;

        if(localBitIndexStart > maxSeqLocIndex) {
            int longIndexEnd = longIndexStart + 1;
            int localShiftStart = 64 - localBitIndexStart;
            int localShiftEnd = bitsPerEntry - localShiftStart;
            this.data[longIndexEnd] = this.data[longIndexEnd] >>> localShiftEnd << localShiftEnd | ((long) value >> localShiftStart);
        }
    }

    public final int get(int index) {
        if (longLen == 0) return 0;
        int bitIndexStart = index * bitsPerEntry;

        int longIndexStart = bitIndexStart >> 6;

        int localBitIndexStart = bitIndexStart & 63;
        if(localBitIndexStart <= maxSeqLocIndex) {
            return (int)(this.data[longIndexStart] >>> localBitIndexStart & mask);
        } else {
            int localShift = 64 - localBitIndexStart;
            return (int) ((this.data[longIndexStart] >>> localBitIndexStart | this.data[longIndexStart + 1] << localShift) & mask);
        }
    }

    public int getLength() {
        return longLen;
    }
    
    public final void fromRaw(int[] arr) {
        final long[] data = this.data;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        long l = 0;
        for (int i = 0; i < longLen; i++) {
            int lastVal;
            for (; localStart <= maxSeqLocIndex; localStart += bitsPerEntry) {
                lastVal = arr[arrI++];
                l |= ((long) lastVal << localStart);
            }
            if (localStart < 64) {
                if (i != longLen - 1) {
                    lastVal = arr[arrI++];
                    int shift = 64 - localStart;

                    long nextVal = lastVal >> shift;

                    l |= ((lastVal - (nextVal << shift)) << localStart);

                    data[i] = l;
                    data[i + 1] = l = nextVal;

                    localStart -= maxSeqLocIndex;
                }
            } else {
                localStart = 0;
                data[i] = l;
                l = 0;
            }
        }
    }

    public final int[] toRaw() {
        return toRaw(new int[4096]);
    }

    public final int[] toRaw(int[] buffer) {
        final long[] data = this.data;
        final int dataLength = longLen;
        final int bitsPerEntry = this.bitsPerEntry;
        final long maxEntryValue = this.mask;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        for (int i = 0; i < dataLength; i++) {
            long l = data[i];
            char lastVal;
            for (; localStart <= maxSeqLocIndex; localStart += bitsPerEntry) {
                lastVal = (char) (l >>> localStart & maxEntryValue);
                buffer[arrI++] = lastVal;
            }
            if (localStart < 64) {
                if (i != dataLength - 1) {
                    lastVal = (char) (l >>> localStart);
                    localStart -= maxSeqLocIndex;
                    l = data[i + 1];
                    int localShift = bitsPerEntry - localStart;
                    lastVal |= l << localShift;
                    lastVal &= maxEntryValue;
                    buffer[arrI++] = lastVal;
                }
            } else {
                localStart = 0;
            }
        }
        return buffer;
    }

    public final char[] toRaw(char[] buffer) {
        final long[] data = this.data;
        final int dataLength = longLen;
        final int bitsPerEntry = this.bitsPerEntry;
        final long maxEntryValue = this.mask;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int arrI = 0;
        for (int i = 0; i < dataLength; i++) {
            long l = data[i];
            char lastVal;
            for (; localStart <= maxSeqLocIndex; localStart += bitsPerEntry) {
                lastVal = (char) (l >>> localStart & maxEntryValue);
                buffer[arrI++] = lastVal;
            }
            if (localStart < 64) {
                if (i != dataLength - 1) {
                    lastVal = (char) (l >>> localStart);
                    localStart -= maxSeqLocIndex;
                    l = data[i + 1];
                    int localShift = bitsPerEntry - localStart;
                    lastVal |= l << localShift;
                    lastVal &= maxEntryValue;
                    buffer[arrI++] = lastVal;
                }
            } else {
                localStart = 0;
            }
        }
        return buffer;
    }
}
