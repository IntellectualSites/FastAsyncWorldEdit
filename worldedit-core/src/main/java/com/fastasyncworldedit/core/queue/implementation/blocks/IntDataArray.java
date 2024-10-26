package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;
import java.util.function.BinaryOperator;

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
    public <V> void processSet(final DataArray get, final BinaryOperator<? extends Vector<V>> op) {
        // TODO is this sane???
        BinaryOperator<IntVector> cOp = cast(op);
        VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
        IntDataArray setArr = this;
        IntDataArray getArr = (IntDataArray) get;
        for (int i = 0; i < CHUNK_SECTION_SIZE; i += species.length()) {
            IntVector vectorSet = IntVector.fromArray(species, setArr.data, i);
            IntVector vectorGet = IntVector.fromArray(species, getArr.data, i);
            vectorSet = cOp.apply(vectorSet, vectorGet);
            vectorSet.intoArray(setArr.data, i);
        }

    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof IntDataArray other)) {
            return false;
        }
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @SuppressWarnings("unchecked")
    private static BinaryOperator<IntVector> cast(final BinaryOperator<? extends Vector<?>> op) {
        return (BinaryOperator<IntVector>) op;
    }

}
