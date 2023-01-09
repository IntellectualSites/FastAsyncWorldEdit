package com.fastasyncworldedit.core.util.io;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryFileSupportTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            1, 2
            2, 3
            2, 4
            3, 5
            8, 255
            8, 256
            9, 257
            """)
    void testBitsPerEntry(int expected, int entries) {
        assertEquals(expected, MemoryFileSupport.bitsPerEntry(entries));
    }

}
