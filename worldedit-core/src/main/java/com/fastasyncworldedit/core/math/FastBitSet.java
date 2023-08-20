package com.fastasyncworldedit.core.math;

import java.math.BigInteger;
import java.util.Arrays;

public class FastBitSet {

    private int size;
    private long[] bits;

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

    public static void set(long[] bits, final int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public static void clear(long[] bits, final int i) {
        bits[i >> 6] &= ~(1L << (i & 0x3F));
    }

    public static void set(long[] bits, final int i, final boolean v) {
        if (v) {
            set(bits, i);
        } else {
            clear(bits, i);
        }
    }

    public static void setRange(long[] bits, final int b, final int e) {
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

    public static void clearRange(long[] bits, final int b, final int e) {
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

    public static void setAll(long[] bits) {
        Arrays.fill(bits, -1L);
    }

    public static void unsetAll(long[] bits) {
        Arrays.fill(bits, 0);
    }

    public static void and(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i) {
            bits[i] &= other[i];
        }
    }

    public static void or(long[] bits, final long[] other) {
        final int end = Math.min(other.length, bits.length);
        for (int i = 0; i < end; ++i) {
            bits[i] |= other[i];
        }
    }

    private static void fill(final long[] a, final int b, final int e, final long l) {
        for (int i = b; i < e; ++i) {
            a[i] = l;
        }
    }

    public void expandTo(int newSize, boolean value) {
        //System.out.println(newSize);
        int newLength = (newSize + 64) >> 6;
        if (newLength <= this.bits.length) {
            if (this.size > newSize) {
                this.size = newSize;
            }
            return;
        }
        long[] tmp = new long[newLength];
        if (value) {
            Arrays.fill(tmp, -1L);
        }
        System.arraycopy(bits, 0, tmp, 0, bits.length);
        this.bits = tmp;
        this.size = newSize;
    }

    public void setAll() {
        setAll(bits);
    }

    public boolean get(final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }

    public void set(final int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public void clear(final int i) {
        bits[i >> 6] &= ~(1L << (i & 0x3F));
    }

    public void set(final int i, final boolean v) {
        if (v) {
            set(i);
        } else {
            clear(i);
        }
    }

    public void setRange(final int b, final int e) {
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

    public void clearRange(final int b, final int e) {
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

    public void and(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i) {
            bits[i] &= other.bits[i];
        }
    }

    public void or(final FastBitSet other) {
        final int end = Math.min(other.bits.length, bits.length);
        for (int i = 0; i < end; ++i) {
            bits[i] |= other.bits[i];
        }
    }

    public int cardinality() {
        if (size == 0) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < bits.length - 1; ++i) {
            count += Long.bitCount(bits[i]);
        }
        return count + Long.bitCount(bits[bits.length - 1] & ~(-1L << (size & 0x3F)));
    }

    public int size() {
        return size;
    }

    public IntIterator iterator() {
        return new IntIterator();
    }

    public class IntIterator {

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
            final int bitIndex = Long.bitCount(lowBit - 1);
            value = ((index - 1) << 6) + bitIndex;
            bitBuffer = bitBuffer ^ lowBit;
            return true;
        }

        public int getValue() {
            return value;
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.bits.length; i++) {
            String bits = new StringBuilder(String.format("%064d", new BigInteger(Long.toBinaryString(this.bits[i]))))
                    .reverse()
                    .toString();
            builder.append(i * 64).append(":").append(bits).append(" ");
        }
        return builder.toString();
    }

}
