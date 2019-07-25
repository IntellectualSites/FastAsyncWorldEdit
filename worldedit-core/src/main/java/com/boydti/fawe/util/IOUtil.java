package com.boydti.fawe.util;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOUtil {

    public static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
    }

    public static void writeInt(OutputStream out, int v) throws IOException {
        out.write(v >>> 24 & 0xFF);
        out.write(v >>> 16 & 0xFF);
        out.write(v >>>  8 & 0xFF);
        out.write(v >>>  0 & 0xFF);
    }

    public static int readVarInt(InputStream in) throws IOException {
        int i = 0;
        int offset = 0;
        int b;
        while ((b = in.read()) > 127) {
            i |= b - 128 << offset;
            offset += 7;
        }
        i |= b << offset;
        return i;
    }

    public static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.write(value & 127 | 128);
            value >>>= 7;
        }
        out.write(value);
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        while (true) {
            int r = in.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }
    }

    public static int copy(InputStream in, OutputStream out, int len) throws IOException {
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

    public static void copy(InputStream in, DataOutput out) throws IOException {
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
