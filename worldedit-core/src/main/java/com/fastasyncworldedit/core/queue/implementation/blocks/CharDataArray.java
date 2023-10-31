package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.Arrays;

final class CharDataArray implements DataArray {
    static final boolean CAN_USE_CHAR_ARRAY = BlockTypesCache.states.length < Character.MAX_VALUE;
    private final char[] data;

    public CharDataArray() {
        this.data = new char[CHUNK_SECTION_SIZE];
    }

    @Override
    public int getAt(final int index) {
        return this.data[index];
    }

    @Override
    public void setAt(final int index, final int value) {
        this.data[index] = (char) value;
    }

    @Override
    public void setRange(final int start, final int end, final int value) {
        Arrays.fill(this.data, start, end, (char) value);
    }

    @Override
    public void setAll(final int value) {
        Arrays.fill(this.data, (char) value);
    }

    @Override
    public void copyInto(final DataArray other) {
        assert other.getClass() == CharDataArray.class;
        final char[] otherData = ((CharDataArray) other).data;
        System.arraycopy(this.data, 0, otherData, 0, CHUNK_SECTION_SIZE);
    }

    @Override
    public boolean isEmpty() {
        return Arrays.equals(this.data, FaweCache.INSTANCE.EMPTY_CHAR_4096);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CharDataArray other)) {
            return false;
        }
        return Arrays.equals(this.data, other.data);
    }

}
