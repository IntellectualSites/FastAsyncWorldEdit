package com.fastasyncworldedit.core.util.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.random.RandomGenerator;

class MemoryFileTest {

    @Test
    void writeALot(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("data.tmp");
        int entries = Integer.MAX_VALUE - 10;
        RandomGenerator generator = RandomGenerator.getDefault();
        try (MemoryFile memoryFile = MemoryFile.create(file, entries, 256)) {
            for (int i = 0; i < entries; i++) {
                memoryFile.setValue(i, generator.nextInt(256));
            }
        }
    }

}
