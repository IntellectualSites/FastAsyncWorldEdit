package net.jpountz.lz4;

import java.io.IOException;
import java.io.InputStream;

public class LZ4InputStream extends InputStream {

    private static LZ4Factory factory = LZ4Factory.fastestInstance();

    private final InputStream inputStream;
    private final LZ4Decompressor decompressor;

    private byte compressedBuffer[];
    private byte decompressedBuffer[];
    private int decompressedBufferPosition = 0;
    private int decompressedBufferLength = 0;

    public LZ4InputStream(InputStream stream) {
        this(stream, 1048576);
    }

    public LZ4InputStream(InputStream stream, int size) {
        this.decompressor = factory.decompressor();
        this.inputStream = stream;
        compressedBuffer = new byte[size];
        decompressedBuffer = new byte[size];
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public int read() throws IOException {
        if (ensureBytesAvailableInDecompressedBuffer())
            return decompressedBuffer[decompressedBufferPosition++] & 0xFF;

        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!ensureBytesAvailableInDecompressedBuffer())
            return -1;

        int numBytesRemainingToRead = len - off;

        while (numBytesRemainingToRead > 0 && ensureBytesAvailableInDecompressedBuffer()) {
            int numBytesToRead = numBytesRemainingToRead;
            int numBytesRemainingInBlock = decompressedBufferLength - decompressedBufferPosition;
            if (numBytesToRead > numBytesRemainingInBlock) {
                numBytesToRead = numBytesRemainingInBlock;
            }

            System.arraycopy(decompressedBuffer, decompressedBufferPosition, b, off, numBytesToRead);

            decompressedBufferPosition += numBytesToRead;
            off += numBytesToRead;
            numBytesRemainingToRead -= numBytesToRead;
        }

        return len - numBytesRemainingToRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long numBytesRemainingToSkip = n;

        while (numBytesRemainingToSkip > 0 && ensureBytesAvailableInDecompressedBuffer()) {
            long numBytesToSkip = numBytesRemainingToSkip;
            int numBytesRemainingInBlock = decompressedBufferLength - decompressedBufferPosition;
            if (numBytesToSkip > numBytesRemainingInBlock) {
                numBytesToSkip = numBytesRemainingInBlock;
            }

            numBytesRemainingToSkip -= numBytesToSkip;
            decompressedBufferPosition += numBytesToSkip;
        }
        return n - numBytesRemainingToSkip;
    }

    private boolean ensureBytesAvailableInDecompressedBuffer() throws IOException {
        while (decompressedBufferPosition >= decompressedBufferLength) {
            if (!fillBuffer()) {
                return false;
            }
        }

        return true;
    }

    private boolean fillBuffer() throws IOException {
        decompressedBufferLength = LZ4StreamHelper.readLength(inputStream);
        int compressedBufferLength = LZ4StreamHelper.readLength(inputStream);

        if (blockHeadersIndicateNoMoreData(compressedBufferLength, decompressedBufferLength)) {
            return false;
        }

        ensureBufferCapacity(compressedBufferLength, decompressedBufferLength);

        if (fillCompressedBuffer(compressedBufferLength)) {
            decompressor.decompress(compressedBuffer, 0, decompressedBuffer, 0, decompressedBufferLength);
            decompressedBufferPosition = 0;
            return true;
        }

        return false;
    }

    private boolean blockHeadersIndicateNoMoreData(int compressedBufferLength, int decompressedBufferLength) {
        return compressedBufferLength < 0 || decompressedBufferLength < 0;
    }

    private boolean fillCompressedBuffer(int compressedBufferLength) throws IOException {
        int bytesRead = 0;
        while (bytesRead < compressedBufferLength) {
            int bytesReadInAttempt = inputStream.read(compressedBuffer, bytesRead, compressedBufferLength - bytesRead);
            if (bytesReadInAttempt < 0)
                return false;
            bytesRead += bytesReadInAttempt;
        }

        return true;
    }

    private void ensureBufferCapacity(int compressedBufferLength, int decompressedBufferLength) {
        if (compressedBufferLength > compressedBuffer.length) {
            compressedBuffer = new byte[compressedBufferLength];
        }

        if (decompressedBufferLength > decompressedBuffer.length) {
            decompressedBuffer = new byte[decompressedBufferLength];
        }
    }

}