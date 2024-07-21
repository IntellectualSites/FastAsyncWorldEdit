package com.fastasyncworldedit.core.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * Basically {@link com.sk89q.worldedit.internal.util.VarIntIterator} but backed by {@link java.io.InputStream}
 */
public class VarIntStreamIterator implements PrimitiveIterator.OfInt {

    private final InputStream parent;
    private final int limit;
    private int index;
    private boolean hasNextInt;
    private int nextInt;

    public VarIntStreamIterator(final InputStream parent, int limit) {
        this.parent = parent;
        this.limit = limit;
    }

    @Override
    public boolean hasNext() {
        if (hasNextInt) {
            return true;
        }
        if (index >= limit) {
            return false;
        }

        try {
            nextInt = readNextInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return hasNextInt = true;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        hasNextInt = false;
        return nextInt;
    }


    private int readNextInt() throws IOException {
        int value = 0;
        for (int bitsRead = 0; ; bitsRead += 7) {
            if (index >= limit) {
                throw new IllegalStateException("Ran out of bytes while reading VarInt (probably corrupted data)");
            }
            byte next = (byte) this.parent.read();
            index++;
            value |= (next & 0x7F) << bitsRead;
            if (bitsRead > 7 * 5) {
                throw new IllegalStateException("VarInt too big (probably corrupted data)");
            }
            if ((next & 0x80) == 0) {
                break;
            }
        }
        return value;
    }

}
