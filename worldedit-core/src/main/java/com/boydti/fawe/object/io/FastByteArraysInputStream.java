package com.boydti.fawe.object.io;

import java.io.InputStream;

public class FastByteArraysInputStream extends InputStream {
    private final byte[][] buffers;
    private final int length;

    private byte[] current;

    private int layer;
    private int localIndex;
    private int globalIndex;
    private int curLen;


    public FastByteArraysInputStream(byte[][] buffers) {
        this.buffers = buffers;
        int size = 0;
        for (byte[] bytes : buffers) {
            size += bytes.length;
        }
        this.length = size;
        current = buffers.length == 0 ? new byte[layer++] : buffers[layer++];
        curLen = current.length;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void close() {
    }

    @Override
    public void mark(int dummy) {
    }

    @Override
    public int available() {
        return this.length - this.globalIndex;
    }

    @Override
    public long skip(long n) {
        if (n <= this.length - this.globalIndex) {
            this.globalIndex += (int) n;
            this.localIndex += (int) n;
            ensureBuffer();
            return n;
        }
        n = this.length - this.globalIndex;
        layer = buffers.length - 1;
        this.current = buffers[layer];
        this.curLen = current.length;
        this.localIndex = current.length;
        this.globalIndex = this.length;
        return n;
    }

    @Override
    public int read() {
        if (curLen != localIndex) {
            globalIndex++;
            return this.current[localIndex++] & 0xFF;
        } else if (length == globalIndex) {
            return -1;
        } else {
            localIndex = 0;
            current = buffers[layer++];
            curLen = current.length;
            globalIndex++;
            return this.current[localIndex++] & 0xFF;
        }
    }

    @Override
    public int read(byte[] b, int offset, int length) {
        if (this.length <= this.globalIndex) {
            return length == 0 ? 0 : -1;
        }
        int n = Math.min(length, this.length - this.globalIndex);
        int read = 0;
        int amount = Math.min(curLen - localIndex, n - read);
        System.arraycopy(this.current, localIndex, b, offset + read, amount);
        read += amount;
        localIndex += amount;
        globalIndex += amount;
        ensureBuffer();
        return read;
    }

    public void ensureBuffer() {
        while (localIndex >= curLen && layer < buffers.length) {
            localIndex -= curLen;
            current = buffers[layer++];
            this.curLen = current.length;
        }
    }

    public long length() {
        return this.length;
    }
}
