package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;

import java.util.Arrays;

final class IntDataArray implements DataArray {
    private final int[] data;

    public IntDataArray() {
        this.data = new int[CHUNK_SECTION_SIZE];
    }

    @Override
    public int getAt(final int index) {
        return this.data[index];
    }

    @Override
    public void setAt(final int index, final int value) {
        this.data[index] = value;
    }

    @Override
    public void setRange(final int start, final int end, final int value) {
        Arrays.fill(this.data, start, end, value);
    }

    @Override
    public void setAll(final int value) {
        Arrays.fill(this.data, value);
    }

    @Override
    public void copyInto(final DataArray other) {
        assert other.getClass() == IntDataArray.class;
        final int[] otherData = ((IntDataArray) other).data;
        System.arraycopy(this.data, 0, otherData, 0, CHUNK_SECTION_SIZE);
    }

    @Override
    public boolean isEmpty() {
        return Arrays.equals(this.data, FaweCache.INSTANCE.EMPTY_INT_4096);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof IntDataArray other)) {
            return false;
        }
        return Arrays.equals(this.data, other.data);
    }

}
