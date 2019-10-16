package com.boydti.fawe.object.collection;

import java.util.Arrays;

public class ChunkBitSet {
    private final static int CHUNK_LAYERS = 16;
    private final static int BITS_PER_LAYER = 4096;
    private final static int BITS_PER_WORD = 6;
    private final static int WORDS = BITS_PER_LAYER >> BITS_PER_WORD;
    private final static IRow NULL_ROW_X = new NullRowX();
    private final static IRow NULL_ROW_Z = new NullRowZ();
    private final static IRow NULL_ROW_Y = new NullRowY();

    private final IRow[] rows;

    public ChunkBitSet() {
        this(16);
    }

    public ChunkBitSet(int size) {
        this.rows = new IRow[size];
        for (int i = 0; i < size; i++) rows[i] = NULL_ROW_X;
    }

    public boolean get(int x, int y, int z) {
        return rows[x >> 4].get(this.rows, x, y, z);
    }

    public void set(int x, int y, int z) {
        rows[x >> 4].set(this.rows, x, y, z);
    }

    public void clear(int x, int y, int z) {
        rows[x >> 4].clear(this.rows, x, y, z);
    }

    private interface IRow {
        default boolean get(IRow[] rows, int x, int y, int z) { return false; }
        void set(IRow[] rows, int x, int y, int z);
        default void clear(IRow[] rows, int x, int y, int z) { return; }
    }

    private static class NullRowX implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowX(parent.length);
            parent[x >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    private static class NullRowZ implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowZ();
            parent[z >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    private static class NullRowY implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowY();
            parent[y >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    private static class RowX implements IRow {
        private final IRow[] rows;

        public RowX(int size) {
            this.rows = new IRow[size];
            for (int i = 0; i < size; i++) rows[i] = NULL_ROW_Z;
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            return rows[z >> 4].get(this.rows, x, y, z);
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            this.rows[z >> 4].set(this.rows, x, y, z);
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            this.rows[z >> 4].clear(this.rows, x, y, z);
        }
    }

    private static class RowZ implements IRow {
        private final IRow[] rows;

        public RowZ() {
            this.rows = new IRow[CHUNK_LAYERS];
            for (int i = 0; i < CHUNK_LAYERS; i++) rows[i] = NULL_ROW_Y;
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            return rows[y >> 4].get(this.rows, x, y, z);
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            this.rows[y >> 4].set(this.rows, x, y, z);
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            this.rows[y >> 4].set(this.rows, x, y, z);
        }
    }

    private static class RowY implements IRow {
        private final long[] bits;

        public RowY() {
            this.bits = new long[WORDS];
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            bits[i >> 6] |= (1L << (i & 0x3F));
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            bits[i >> 6] &= ~(1L << (i & 0x3F));
        }
    }

    private static IRow[] resize(IRow[] arr, IRow def) {
        int len = arr.length;
        int newLen = len == 1 ? 1 : Integer.highestOneBit(len - 1) * 2;
        IRow[] copy = Arrays.copyOf(arr, newLen, IRow[].class);
        for (int i = len; i < newLen; i++) copy[i] = def;
        return copy;
    }
}

