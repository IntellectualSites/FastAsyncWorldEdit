package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.OutputStream;

public class AbstractDelegateOutputStream extends OutputStream {
    private final OutputStream parent;

    @Override
    public void write(int b) throws IOException {
        parent.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        parent.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        parent.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        parent.flush();
    }

    @Override
    public void close() throws IOException {
        parent.close();
    }

    public AbstractDelegateOutputStream(OutputStream os) {
        this.parent = os;
    }
}
