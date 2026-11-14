package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.Filter;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorizedPatternTest {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    @ParameterizedTest
    @ValueSource(ints = {1, Short.MAX_VALUE, Short.MAX_VALUE + 1, 34825, Character.MAX_VALUE})
    void applyVectorWritesOrdinal(int ordinal) {
        char[] data = new char[SPECIES.length()];
        applyVector((char) ordinal, data, SPECIES.maskAll(true));

        char[] expected = new char[SPECIES.length()];
        Arrays.fill(expected, (char) ordinal);
        assertEquals(new String(expected), new String(data));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 34825})
    void applyVectorOnlyChangesMaskedLanes(int ordinal) {
        char[] data = new char[SPECIES.length()];
        Arrays.fill(data, (char) 7);
        VectorMask<Short> evenLanes = VectorMask.fromLong(SPECIES, 0x5555555555555555L);
        applyVector((char) ordinal, data, evenLanes);

        for (int i = 0; i < data.length; i++) {
            assertEquals(i % 2 == 0 ? (char) ordinal : (char) 7, data[i], "lane " + i);
        }
    }

    private static void applyVector(char ordinal, char[] data, VectorMask<Short> mask) {
        VectorFacade set = new VectorFacade(null);
        set.setData(data);
        set.setIndex(0);
        new SimdSupport.VectorizedPattern<>(new Filter() {
        }, ordinal).applyVector(new VectorFacade(null), set, mask);
    }

}
