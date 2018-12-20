package com.boydti.fawe.object.collection;

import java.util.Arrays;

public final class FastBitSet {
    private final int size;
    private final long[] bits;

    public FastBitSet(int size) {
        this.size = size;
        bits = new long[(size + 64) >> 6];
    }

    public FastBitSet(long[] bits, int size) {
        this.bits = bits;
        this.size = size;
    }

    public static long[] create(int size) {
        return new long[(size + 64) >> 6];
    }

    public static boolean get(long[] bits, final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }

    public static final void set(long[] bits, final int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public static final void clear(long[] bits, final int i) {
        bits[i >> 6] &= ~(1L << (i & 0x3F));
    }

    public static final void set(long[] bits, final int i, final boolean v) {
        if (v)
            set(bits, i);
        else
            clear(bits, i);
    }

    public static final void setRange(long[] bits, final int b, final int e) {
        final int bt = b >> 6;
        final int et = e >> 6;
        if (bt != et) {
            fill(bits, bt + 1, et, -1L);
            bits[bt] |= (-1L << (b & 0x3F));
            bits[et] |= ~(-1L << (e & 0x3F));
        } else {
            bits[bt] |= ((-1L << (b & 0x3F)) & ~(-1L << (e & 0x3F)));
        }
    }

    public static final void clearRange(long[] bits, final int b, final int e) {
        final int bt = b >> 6;
        final int et = e >> 6;
        if (bt != et) {
            fill(bits, bt + 1, et, 0L);
            bits[bt] &= ~(-1L << (b & 0x3F));
            bits[et] &= (-1L << (e & 0x3F));
        } else {
            bits[bt] &= (~(-1L << (b & 0x3F)) | (-1L << (e & 0x3F)));
        }
    }

    public static final void setAll(long[] bits) {
        Arrays.fill(bits, -1L);
    }

    public static final void clearAll(long[] bits) {
        Arrays.fill(bits, 0L);
    }

    public static final void invertAll(long[] bits) {
        for (int i = 0; i < bits.length; ++i)
            bits[i] = ~bits[i];
    }

    public static final void and(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] &= other[i];
    }

    public static final void or(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] |= other[i];
    }

    public static final void nand(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] = ~(bits[i] & other[i]);
    }

    public static final void nor(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] = ~(bits[i] | other[i]);
    }

    public static final void xor(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] ^= other[i];
    }

    public static final long memoryUsage(long[] bits) {
        return 8L * bits.length;
    }

    public final boolean get(final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }

    public final void set(final int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public final void clear(final int i) {
        bits[i >> 6] &= ~(1L << (i & 0x3F));
    }

    public final void set(final int i, final boolean v) {
        if (v)
            set(i);
        else
            clear(i);
    }

    public final void setRange(final int b, final int e) {
        final int bt = b >> 6;
        final int et = e >> 6;
        if (bt != et) {
            fill(bits, bt + 1, et, -1L);
            bits[bt] |= (-1L << (b & 0x3F));
            bits[et] |= ~(-1L << (e & 0x3F));
        } else {
            bits[bt] |= ((-1L << (b & 0x3F)) & ~(-1L << (e & 0x3F)));
        }
    }

    public final void clearRange(final int b, final int e) {
        final int bt = b >> 6;
        final int et = e >> 6;
        if (bt != et) {
            fill(bits, bt + 1, et, 0L);
            bits[bt] &= ~(-1L << (b & 0x3F));
            bits[et] &= (-1L << (e & 0x3F));
        } else {
            bits[bt] &= (~(-1L << (b & 0x3F)) | (-1L << (e & 0x3F)));
        }
    }

    public final void setAll() {
        Arrays.fill(bits, -1L);
    }

    public final void clearAll() {
        Arrays.fill(bits, 0L);
    }

    public final void invertAll() {
        for (int i = 0; i < bits.length; ++i)
            bits[i] = ~bits[i];
    }

    public final void and(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] &= other.bits[i];
    }

    public final void or(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] |= other.bits[i];
    }

    public final void nand(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] = ~(bits[i] & other.bits[i]);
    }

    public final void nor(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] = ~(bits[i] | other.bits[i]);
    }

    public final void xor(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i)
            bits[i] ^= other.bits[i];
    }

    public final int cardinality() {
        if (size == 0) return 0;
        int count = 0;
        for (int i = 0; i < bits.length - 1; ++i)
            count += Long.bitCount(bits[i]);
        return count + Long.bitCount(bits[bits.length - 1] & ~(-1L << (size & 0x3F)));
    }

    public final int size() {
        return size;
    }

    public final long memoryUsage() {
        return 8L * bits.length;
    }

    private static void fill(final long[] a, final int b, final int e, final long l) {
        for (int i = b; i < e; ++i)
            a[i] = l;
    }

    public static long calculateMemoryUsage(int entries) {
        final int numLongs = (entries + 64) >> 6;
        return 8L * numLongs;
    }

    public IntIterator iterator() {
        return new IntIterator();
    }

    public final class IntIterator {
        int index = 0;
        long bitBuffer = 0;
        int value = 0;

        public boolean next() {
            while (bitBuffer == 0) {
                if (index >= bits.length) {
                    return false;
                }
                bitBuffer = bits[index];
                index++;
            }
            final long lowBit = Long.lowestOneBit(bitBuffer);
            final int bitIndex = Long.bitCount(lowBit-1);
            value = ((index-1)<<6)+bitIndex;
            bitBuffer = bitBuffer ^ lowBit;
            return true;
        }

        public int getValue() {
            return value;
        }
    }
}