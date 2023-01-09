package com.fastasyncworldedit.core.util.io;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static com.fastasyncworldedit.core.util.io.MemoryFileSupport.shift;

final class SmallMemoryFile implements MemoryFile {
    private final FileChannel channel;
    private final MappedByteBuffer buffer;
    private final int bitsPerEntry;
    private final int entryMask;

    SmallMemoryFile(FileChannel channel, int size, final int bitsPerEntry) throws IOException {
        this.channel = channel;
        this.buffer = this.channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.bitsPerEntry = bitsPerEntry;
        this.entryMask = (1 << this.bitsPerEntry) - 1;
    }

    @Override
    public void setValue(int index, int value) {
        long bitPos = bitPos(index);
        int bytePos = toBytePos(bitPos);
        int shift = shift(bitPos, bytePos);
        write(bytePos, shift, value);
    }

    @Override
    public int getValue(final int index) {
        long bitPos = bitPos(index);
        int bytePos = toBytePos(bitPos);
        int shift = shift(bitPos, bytePos);
        return read(bytePos, shift);
    }

    private void write(int bytePos, int shift, int value) {
        int mask = this.entryMask << shift;
        int existing = this.buffer.getInt(bytePos);
        int result = (existing & ~mask) | (value << shift);
        this.buffer.putInt(bytePos, result);
    }

    private int read(int bytePos, int shift) {
        return (this.buffer.getInt(bytePos) >> shift) & this.entryMask;
    }

    private static int toBytePos(long bitPos) {
        return (int) (bitPos >> 3); // must be in int range for SmallMemoryFile
    }

    private long bitPos(int index) {
        return (long) this.bitsPerEntry * index;
    }

    @Override
    public void close() throws IOException {
        flush();
        this.channel.close();
    }

    @Override
    public void flush() {
        this.buffer.force();
    }

}
