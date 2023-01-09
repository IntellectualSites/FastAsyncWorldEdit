package com.fastasyncworldedit.core.util.io;

import java.io.Flushable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * A memory-mapped file that can hold integer values in range from 2 up to a variable maximum.
 */
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

    static MemoryFile load(Path file, int valueCount) throws IOException {
        FileChannel channel = FileChannel.open(file, MemoryFileSupport.OPTIONS);
        long size = channel.size();
        if (size <= Integer.MAX_VALUE) {
            int bitsPerEntry = MemoryFileSupport.bitsPerEntry(valueCount);
            return new SmallMemoryFile(channel, (int) size, bitsPerEntry);
        }
        throw new UnsupportedEncodingException("existing file too large: " + size);
    }

    void setValue(int index, int value);

    int getValue(int index);

    /**
     * {@inheritDoc}
     */
    @Override
    void close() throws IOException;

}
