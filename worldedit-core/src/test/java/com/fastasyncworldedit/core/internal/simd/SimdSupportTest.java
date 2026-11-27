package com.fastasyncworldedit.core.internal.simd;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimdSupportTest {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    @ParameterizedTest
    @ValueSource(ints = {0, 1, Short.MAX_VALUE, Short.MAX_VALUE + 1, 34825, Character.MAX_VALUE})
    void broadcastChar(int value) {
        char[] data = new char[SPECIES.length()];
        SimdSupport.broadcast(SPECIES, (char) value).intoCharArray(data, 0);

        char[] expected = new char[SPECIES.length()];
        Arrays.fill(expected, (char) value);
        assertEquals(new String(expected), new String(data));
    }

}
