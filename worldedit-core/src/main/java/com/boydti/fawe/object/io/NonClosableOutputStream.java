package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.OutputStream;

public class NonClosableOutputStream extends AbstractDelegateOutputStream {

    public NonClosableOutputStream(OutputStream os) {
        super(os);
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }
}
