package com.boydti.fawe.object.io;

import java.io.InputStream;

public class FastByteArrayInputStream extends InputStream {
    public byte[] array;
    public int offset;
    public int length;
    private int position;
    private int mark;

    public FastByteArrayInputStream(byte[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    public FastByteArrayInputStream(byte[] array) {
        this(array, 0, array.length);
    }

    public boolean markSupported() {
        return true;
    }

    public void reset() {
        this.position = this.mark;
    }

    public void close() {
    }

    public void mark(int dummy) {
        this.mark = this.position;
    }

    public int available() {
        return this.length - this.position;
    }

    public long skip(long n) {
        if (n <= this.length - this.position) {
            this.position += (int) n;
            return n;
        }
        n = this.length - this.position;
        this.position = this.length;
        return n;
    }

    public int read() {
        if (this.length == this.position) {
            return -1;
        }
        return this.array[(this.offset + this.position++)] & 0xFF;
    }

    public int read(byte[] b, int offset, int length) {
        if (this.length == this.position) {
            return length == 0 ? 0 : -1;
        }
        int n = Math.min(length, this.length - this.position);
        System.arraycopy(this.array, this.offset + this.position, b, offset, n);
        this.position += n;
        return n;
    }

    public long position() {
        return this.position;
    }

    public void position(long newPosition) {
        this.position = ((int) Math.min(newPosition, this.length));
    }

    public long length() {
        return this.length;
    }
}
