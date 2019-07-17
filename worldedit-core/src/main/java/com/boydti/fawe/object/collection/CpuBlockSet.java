//package com.boydti.fawe.object.collection;
//
//import com.sk89q.worldedit.math.BlockVector2;
//import com.sk89q.worldedit.math.BlockVector3;
//
//import java.util.Iterator;
//import java.util.Set;
//
//public final class CpuBlockSet extends BlockSet {
//    private static final int DIRTY_SET = 0x1;
//    private static final int DIRTY_CLEAR = 0x10;
//
//    public static int WORLD_HEIGHT = 256;
//    private final long[] bits;
//    private final byte[] dirty;
//    private final int chunkShift;
//    private final int chunkShift2;
//
//    public CpuBlockSet(int size, int offsetX, int offsetZ) {
//        super(offsetX, offsetZ);
//        size = size == 1 ? 1 : Integer.highestOneBit(size - 1) * 2;
//        int arrayLen = (size * size * WORLD_HEIGHT) >> 6;
//        int bitShift = Integer.bitCount(Integer.highestOneBit(size) - 1);
//        this.chunkShift = 12 + bitShift;
//        this.chunkShift2 = 12 + bitShift * 2;
//        this.bits = new long[arrayLen];
//        this.dirty = new byte[arrayLen >> 12];
//    }
//
//    @Override
//    public BlockVector3 getMinimumPoint() {
//
//    }
//
//    @Override
//    public BlockVector3 getMaximumPoint() {
//        // visited set
//        // queue (longs)
//    }
//
//    @Override
//    public Iterator<BlockVector3> iterator() {
//
//    }
//
//    @Override
//    public Set<BlockVector2> getChunks() {
//        // next 65536
//    }
//
//    @Override
//    public Set<BlockVector3> getChunkCubes() {
//        // next 4096
//    }
//
//    private final boolean contains(final int i) {
//        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
//    }
//
//    private final boolean add(final int i) {
//        int offset = i >> 6;
//        long value = bits[offset];
//        long mask = (1L << (i & 0x3F));
//        if ((value & mask) == 0) {
//            dirty[i >> 16] |= DIRTY_SET;
//            bits[offset] = value | mask;
//            return true;
//        }
//        return false;
//    }
//
//    private final boolean remove(final int i) {
//        int offset = i >> 6;
//        long value = bits[offset];
//        long mask = (1L << (i & 0x3F));
//        if ((value & mask) != 0) {
//            bits[offset] = value & ~mask;
//            dirty[i >> 16] |= DIRTY_CLEAR;
//            return true;
//        }
//        return false;
//    }
//
//    private final void set(final int i) {
//        bits[i >> 6] |= (1L << (i & 0x3F));
//        dirty[i >> 12] |= DIRTY_SET;
//    }
//
//    private final void clear(final int i) {
//        bits[i >> 6] &= ~(1L << (i & 0x3F));
//        dirty[i >> 12] |= DIRTY_CLEAR;
//    }
//
//    @Override
//    public boolean contains(int x, int y, int z) {
//        return contains(index(x, y, z));
//    }
//
//    @Override
//    public boolean add(int x, int y, int z) {
//        return add(index(x, y, z));
//    }
//
//    @Override
//    public void set(int x, int y, int z) {
//        set(index(x, y, z));
//    }
//
//    @Override
//    public void clear(int x, int y, int z) {
//        clear(index(x, y, z));
//    }
//
//    @Override
//    public boolean remove(int x, int y, int z) {
//        return remove(index(x, y, z));
//    }
//
//    @Override
//    public void clear() {
//        for (int i = 0; i < dirty.length; i++) {
//            boolean isDirty = dirty[i] != 0;
//            if (isDirty) {
//                dirty[i] = 0;
//                int start = i << 10;
//                int end = Math.min(bits.length, (i + 1) << 10);
//                for (int j = start; j < end; j++) {
//                    bits[j] = 0;
//                }
//            }
//        }
//    }
//
//    private final int index(int x, int y, int z) {
//        return (((y & 15) << 8) | ((z & 15) << 4) | (x & 15)) | ((y >> 4) << chunkShift2) | ((z >> 4) << chunkShift) | ((x >> 4) << 12);
//    }
//}
