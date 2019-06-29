package com.boydti.fawe.object;

import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FaweOutputStream extends DataOutputStream {

    private final OutputStream parent;

    public FaweOutputStream(OutputStream parent) {
        super(parent);
        this.parent = parent;
    }

    public OutputStream getParent() {
        return parent;
    }

    public void write(int b, int amount) throws IOException {
        for (int i = 0; i < amount; i++) {
            write(b);
        }
    }

    public void writeShort(short s) throws IOException {
        write((byte) (s >>> 8));
        write((byte) (s));
    }

    public void writeMedium(int m) throws IOException {
        write((byte) (m >>> 16));
        write((byte) (m >>> 8));
        write((byte) (m));
    }

    public void writeVarInt(int i) throws IOException {
        while((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }
        this.writeByte(i);
    }

    public void write(long[] data) throws IOException {
        this.writeVarInt(data.length);

        for (long datum : data) {
            this.writeLong(datum);
        }
    }

    private NBTOutputStream nbtOut;

    public void writeNBT(String name, Tag tag) throws IOException {
        if (nbtOut == null) {
            nbtOut = new NBTOutputStream(parent);
        }
        nbtOut.writeNamedTag(name, tag);
    }

    public void writePrimitive(Object value) throws IOException {
        Class<? extends Object> clazz = value.getClass();
        if (clazz == long.class || clazz == Long.class) {
            writeLong((long) value);
        } else if (clazz == double.class || clazz == Double.class) {
            writeDouble((double) value);
        } else if (clazz == float.class || clazz == Float.class) {
            writeFloat((float) value);
        } else if (clazz == int.class || clazz == Integer.class) {
            writeInt((int) value);
        } else if (clazz == short.class || clazz == Short.class) {
            writeShort((short) value);
        } else if (clazz == char.class || clazz == Character.class) {
            writeChar((char) value);
        } else if (clazz == byte.class || clazz == Byte.class) {
            writeByte((byte) value);
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            writeBoolean((boolean) value);
        } else {
            throw new UnsupportedOperationException("Unknown class " + clazz);
        }
    }

    @Override
    public void close() throws IOException {
        if (nbtOut != null) {
            nbtOut.close();
        }
        parent.close();
    }
}
