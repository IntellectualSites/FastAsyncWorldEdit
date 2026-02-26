package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

final class MemorySegmentBasedDataArray implements DataArray {
    private static final VarHandle ACCESSOR = createAccessor();

    private static VarHandle createAccessor() {
        VarHandle arrayElement = ValueLayout.JAVA_INT.arrayElementVarHandle();
        // we don't need the base offset, it's always zero in our case
        arrayElement = MethodHandles.insertCoordinates(arrayElement, 1, 0L);
        return arrayElement.withInvokeExactBehavior();
    }

    private final MemorySegment data;

    MemorySegmentBasedDataArray(boolean readOnly) {
        MemorySegment segment = MemorySegment.ofArray(new long[CHUNK_SECTION_SIZE >> 1]);
        this.data = readOnly ? segment.asReadOnly() : segment;
    }

    @Override
    public int getAt(final int index) {
        return (int) ACCESSOR.get(this.data, (long) index);
    }

    @Override
    public void setAt(final int index, final int value) {
        ACCESSOR.set(this.data, (long) index, value);
    }

    @Override
    public void setRange(final int start, final int end, final int value) {
        for (int i = start; i < end; i++) {
            setAt(i, value);
        }
    }

    @Override
    public void setAll(final int value) {
        if (value == 0) {
            this.data.fill((byte) 0);
        } else {
            setRange(0, 4096, value);
        }
    }

    @Override
    public void copyInto(final DataArray other) {
        ((MemorySegmentBasedDataArray) other).data.copyFrom(this.data);
    }

    @Override
    public boolean isEmpty() {
        for (long i = 0; i < this.data.byteSize() >> 3; i++) {
            if (this.data.getAtIndex(ValueLayout.JAVA_LONG, i) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IntVector loadAt(final VectorSpecies<Integer> species, final int index) {
        return IntVector.fromMemorySegment(species, this.data, index * 4L, ByteOrder.nativeOrder());
    }

    @Override
    public void storeAt(final int index, final IntVector vector) {
        vector.intoMemorySegment(this.data, index * 4L, ByteOrder.nativeOrder());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MemorySegmentBasedDataArray other && data.mismatch(other.data) < 0;
    }

}
