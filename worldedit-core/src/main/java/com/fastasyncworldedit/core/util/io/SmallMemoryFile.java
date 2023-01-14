package com.fastasyncworldedit.core.util.io;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static com.fastasyncworldedit.core.util.io.MemoryFileSupport.shift;

/**
 * This implementation of MemoryFile provides storage of up to Integer.MAX_VALUE bytes.
 * As access always uses {@link java.nio.ByteBuffer#getInt(int)}/ {@link java.nio.ByteBuffer#putInt(int, int)},
 * the last three bytes cannot be accessed directly but only by accessing the whole integer.
 * Otherwise, this class makes heavy use of unaligned memory access and a configured
 * {@link ByteOrder#LITTLE_ENDIAN little endian} {@link java.nio.ByteBuffer}.
 * <p/>
 * To read or write a value, the highest byte position that fully contains the value is chosen.
 * At this position, a whole {@code int} is read.
 * <p/>
 * Besides the padding at the end, no bit is unused.
 * Due to the required shifting in combination with the int-based
 * access, values can take up to 25 bits. Above that, information will get lost.
 * <p/>
 * The last byte of the padding is used to store the bitsPerEntry value to allow reading the file again.
 */
final class SmallMemoryFile implements MemoryFile {
    private final FileChannel channel;
    private final MappedByteBuffer buffer;
    private final int bitsPerEntry;
    private final int entryMask;

    SmallMemoryFile(FileChannel channel, int size, final int bitsPerEntry) throws IOException {
        this.channel = channel;
        this.channel.truncate(size); // cut off if previous file was larger
        this.buffer = this.channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.bitsPerEntry = bitsPerEntry;
        this.entryMask = (1 << this.bitsPerEntry) - 1;
    }

    @Override
    public void setValue(long index, int value) {
        long bitPos = bitPos(index);
        int bytePos = toBytePos(bitPos);
        int shift = shift(bitPos, bytePos);
        write(bytePos, shift, value);
    }

    @Override
    public int getValue(final long index) {
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
        return (this.buffer.getInt(bytePos) >>> shift) & this.entryMask;
    }

    private static int toBytePos(long bitPos) {
        return (int) (bitPos >>> 3); // must be in int range for SmallMemoryFile
    }

    private long bitPos(long index) {
        return this.bitsPerEntry * index;
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
