package com.fastasyncworldedit.core.util.io;

import java.io.Flushable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;


public sealed interface MemoryFile extends AutoCloseable, Flushable permits SmallMemoryFile {

    /**
     * {@return a memory-mapped file that can store up to {@code entries} integers in the range of {@code [0, valueCount)}}
     */
    static MemoryFile create(Path file, long entries, int valueCount) throws IOException {
        int bitsPerEntry = MemoryFileSupport.bitsPerEntry(valueCount);
        long bytesNeeded = MemoryFileSupport.requiredBytes(bitsPerEntry, entries);
        if (bytesNeeded <= Integer.MAX_VALUE) {
            return new SmallMemoryFile(FileChannel.open(file, MemoryFileSupport.OPTIONS), (int) bytesNeeded, bitsPerEntry);
        }
        throw new UnsupportedOperationException("too many entries: " + entries);
    }

    void setValue(int index, int value);

    int getValue(int index);

    /**
     * {@inheritDoc}
     */
    @Override
    void close() throws IOException;

}
