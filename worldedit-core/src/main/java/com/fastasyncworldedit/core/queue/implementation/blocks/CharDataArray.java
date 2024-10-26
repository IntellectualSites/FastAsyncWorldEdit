package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;
import java.util.function.BinaryOperator;

final class CharDataArray implements DataArray {
    static final boolean CAN_USE_CHAR_ARRAY = Boolean.getBoolean("fawe.test") || BlockTypesCache.states.length < Character.MAX_VALUE;
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
    public <T> void processSet(final DataArray get, final BinaryOperator<? extends Vector<T>> op) {
        // TODO is this sane???
        BinaryOperator<ShortVector> cOp = cast(op);
        VectorSpecies<Short> species = ShortVector.SPECIES_PREFERRED;
        CharDataArray setArr = this;
        CharDataArray getArr = (CharDataArray) get;
        for (int i = 0; i < CHUNK_SECTION_SIZE; i += species.length()) {
            ShortVector vectorSet = ShortVector.fromCharArray(species, setArr.data, i);
            ShortVector vectorGet = ShortVector.fromCharArray(species, getArr.data, i);
            vectorSet = cOp.apply(vectorSet, vectorGet);
            vectorSet.intoCharArray(setArr.data, i);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CharDataArray other)) {
            return false;
        }
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @SuppressWarnings("unchecked")
    private static BinaryOperator<ShortVector> cast(final BinaryOperator<? extends Vector<?>> op) {
        return (BinaryOperator<ShortVector>) op;
    }

}
