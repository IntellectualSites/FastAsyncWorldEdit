package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessInputStream extends InputStream {
    private final RandomAccessFile raf;

    public RandomAccessInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public int available() throws IOException {
        return (int) (raf.length() - raf.getFilePointer());
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
