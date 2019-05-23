package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;


/**
 * A speedy implementation of ByteArrayOutputStream. It's not synchronized, and it
 * does not copy buffers when it's expanded. There's also no copying of the internal buffer
 * if it's contents is extracted with the writeTo(stream) method.
 *
 * @author Rickard ?berg
 * @author Brat Baker (Atlassian)
 * @author Alexey
 * @version $Date: 2008-01-19 10:09:56 +0800 (Sat, 19 Jan 2008) $ $Id: FastByteArrayOutputStream.java 3000 2008-01-19 02:09:56Z tm_jee $
 */
public class FastByteArrayOutputStream extends OutputStream {

    private static final int DEFAULT_BLOCK_SIZE = 8192;

    private ArrayDeque<byte[]> buffers = new ArrayDeque<>();

    private byte[] buffer;
    private int blockSize;
    private int index;
    private int size;


    public FastByteArrayOutputStream() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public FastByteArrayOutputStream(int aSize) {
        blockSize = aSize;
        buffer = new byte[blockSize];
    }

    public FastByteArrayOutputStream(byte[] buffer) {
        blockSize = buffer.length;
        this.buffer = buffer;
    }

    public int getSize() {
        return size + index;
    }

    public byte[][] toByteArrays() {
        if (index > 0) {
            byte[] buf2 = new byte[index];
            System.arraycopy(buffer, 0, buf2, 0, index);
            buffers.addLast(buf2);
            size += index;
            index = 0;
        }
        byte[][] res = new byte[buffers.size()][];
        int i = 0;
        for (byte[] bytes : buffers) {
            res[i++] = bytes;
        }
        return res;
    }

    public byte[] toByteArray() {
        if (buffers.isEmpty()) {
            if (buffer.length == index) {
                return buffer;
            }
            buffer = Arrays.copyOfRange(buffer, 0, index);
            return buffer;
        }
        byte[] data = new byte[getSize()];

        // Check if we have a list of buffers
        int pos = 0;

        if (buffers != null) {
            for (byte[] bytes : buffers) {
                System.arraycopy(bytes, 0, data, pos, bytes.length);
                pos += bytes.length;
            }
        }

        // write the internal buffer directly
        System.arraycopy(buffer, 0, data, pos, index);

        this.index = size + index;
        this.buffer = data;
        this.buffers.clear();
        return this.buffer;
    }

    public String toString() {
        return new String(toByteArray());
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length > blockSize) {
            if (index > 0) {
                byte[] buf2 = new byte[index];
                System.arraycopy(buffer, 0, buf2, 0, index);
                buffer = buf2;
                addBuffer();
            }
            size += b.length;
            buffers.addLast(b);
        } else {
            write(b, 0, b.length);
        }
    }

    public void write(int datum) {
        if (index == blockSize) {
            addBuffer();
        }
        // store the byte
        buffer[index++] = (byte) datum;
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        if ((offset < 0) || ((offset + length) > data.length) || (length < 0)) {
            throw new IndexOutOfBoundsException();
        } else {
            if ((index + length) > blockSize) {
                int copyLength;

                do {
                    if (index == blockSize) {
                        addBuffer();
                    }

                    copyLength = blockSize - index;

                    if (length < copyLength) {
                        copyLength = length;
                    }

                    System.arraycopy(data, offset, buffer, index, copyLength);
                    offset += copyLength;
                    index += copyLength;
                    length -= copyLength;
                } while (length > 0);
            } else {
                // Copy in the subarray
                System.arraycopy(data, offset, buffer, index, length);
                index += length;
            }
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        // Check if we have a list of buffers
        if (buffers != null) {

            for (byte[] bytes : buffers) {
                out.write(bytes, 0, blockSize);
            }
        }

        // write the internal buffer directly
        out.write(buffer, 0, index);
    }

    public void writeTo(RandomAccessFile out) throws IOException {
        // Check if we have a list of buffers
        if (buffers != null) {

            for (byte[] bytes : buffers) {
                out.write(bytes, 0, blockSize);
            }
        }

        // write the internal buffer directly
        out.write(buffer, 0, index);
    }

    public void writeTo(Writer out, String encoding) throws IOException {
        if (buffers != null) {
            writeToViaSmoosh(out, encoding);
        } else {
            writeToViaString(out, encoding);
        }
    }

    private void writeToViaString(Writer out, String encoding) throws IOException {
        byte[] bufferToWrite = buffer; // this is always the last buffer to write
        int bufferToWriteLen = index;  // index points to our place in the last buffer
        writeToImpl(out, encoding, bufferToWrite, bufferToWriteLen);
    }

    private void writeToViaSmoosh(Writer out, String encoding) throws IOException {
        byte[] bufferToWrite = toByteArray();
        int bufferToWriteLen = bufferToWrite.length;
        writeToImpl(out, encoding, bufferToWrite, bufferToWriteLen);
    }

    private void writeToImpl(Writer out, String encoding, byte[] bufferToWrite, int bufferToWriteLen)
            throws IOException {
        String writeStr;
        if (encoding != null) {
            writeStr = new String(bufferToWrite, 0, bufferToWriteLen, encoding);
        } else {
            writeStr = new String(bufferToWrite, 0, bufferToWriteLen);
        }
        out.write(writeStr);
    }

    private void addBuffer() {
        buffers.addLast(buffer);
        buffer = new byte[blockSize];
        size += index;
        index = 0;
    }
}
