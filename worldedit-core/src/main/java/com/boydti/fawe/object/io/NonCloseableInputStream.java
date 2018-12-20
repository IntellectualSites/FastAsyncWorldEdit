package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.InputStream;

public class NonCloseableInputStream extends InputStream {

    private final InputStream parent;

    public NonCloseableInputStream(InputStream parent) {
        this.parent = parent;
    }
    @Override
    public int read() throws IOException {
        return parent.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return parent.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return parent.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return parent.skip(n);
    }

    @Override
    public int available() throws IOException {
        return parent.available();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void mark(int readlimit) {
        parent.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        parent.reset();
    }

    @Override
    public boolean markSupported() {
        return parent.markSupported();
    }
}
