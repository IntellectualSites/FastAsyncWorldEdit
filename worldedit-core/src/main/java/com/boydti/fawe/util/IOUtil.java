package com.boydti.fawe.util;

import java.io.*;
import java.net.URI;

public final class IOUtil {
    public InputStream toInputStream(URI uri) throws IOException {
        String scheme = uri.getScheme();
        switch (scheme.toLowerCase()) {
            case "file":
                return new FileInputStream(uri.getPath());
            case "http":
            case "https":
                return uri.toURL().openStream();
            default:
                return null;
        }
    }

    public static final int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public static final void writeInt(OutputStream out, int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>>  0) & 0xFF);
    }

    public static final void writeVarInt(final OutputStream out, int i) throws IOException {
        while((i & -128) != 0) {
            out.write(i & 127 | 128);
            i >>>= 7;
        }
        out.write(i);
    }

    public static final int readVarInt(InputStream in) throws IOException {
        int i = 0;
        int offset = 0;
        int b;
        while ((b = in.read()) > 127) {
            i |= (b - 128) << offset;
            offset += 7;
        }
        i |= b << offset;
        return i;
    }

    public static final void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        while (true) {
            int r = in.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }
    }

    public static final int copy(InputStream in, OutputStream out, int len) throws IOException {
        byte[] buf = new byte[8192];
        while (len > 0) {
            int r = in.read(buf, 0, Math.min(buf.length, len));
            if (r == -1) {
                break;
            }
            len -= r;
            out.write(buf, 0, r);
        }
        return len;
    }

    public static final void copy(InputStream in, DataOutput out) throws IOException {
        byte[] buf = new byte[8192];
        while (true) {
            int r = in.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }
    }
}
