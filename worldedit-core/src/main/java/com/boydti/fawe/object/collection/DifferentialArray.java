package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.serialize.Serialize;
import com.boydti.fawe.util.MainUtil;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Records changes made through the {@link #setByte(int, byte)} or {@link #setChar(int, char)} method<br/>
 * If you are editing the raw data, use {@link #record(Runnable)}
 * @param <T>
 */
public final class DifferentialArray<T> implements DifferentialCollection<T> {
    private final byte[] dataBytes;
    private byte[] changesBytes;

    private final int[] dataInts;
    private int[] changesInts;

    private final char[] dataChars;
    private char[] changesChars;

    @Serialize private final T data;

    private T changes;

    private boolean changed;
    private int length;

    public DifferentialArray(T array) {
        checkNotNull(array);
        Class<? extends Object> clazz = array.getClass();
        checkArgument(clazz.isArray(), "Data must be an array");
        checkArgument(clazz.getComponentType().isPrimitive(), "Data must be a primitive array");
        this.data = array;

        if (array instanceof byte[]) {
            dataBytes = (byte[]) array;
            length = dataBytes.length;
        } else {
            dataBytes = null;
        }
        if (array instanceof int[]) {
            dataInts = (int[]) array;
            length = dataInts.length;
        } else {
            dataInts = null;
        }
        if (array instanceof char[]) {
            dataChars = (char[]) array;
            length = dataChars.length;
        } else {
            dataChars = null;
        }
    }

    public void record(Runnable task) {
        if (changes == null) {
            if (data instanceof byte[]) {
                changes = (T) (changesBytes = new byte[length]);
            } else if (data instanceof int[]) {
                changes = (T) (changesInts = new int[length]);
            } else if (data instanceof char[]) {
                changes = (T) (changesChars = new char[length]);
            }
        }
        T tmp;
        boolean changed = this.changed;
        if (changed) {
            tmp = (T) MainUtil.copyNd(data);
        } else {
            tmp = changes;
            System.arraycopy(data, 0, tmp, 0, Array.getLength(data));
        }
        Throwable caught = null;
        try {
            task.run();
        } catch (Throwable e) {
            caught = e;
            task.run();
        }
        if (tmp instanceof int[]) {
            int[] tmpInts = (int[]) tmp;
            for (int i = 0; i < tmpInts.length; i++) {
                int tmpInt = tmpInts[i];
                int dataInt = dataInts[i];
                if (tmpInt != dataInt) {
                    this.changed = true;
                    tmpInts[i] -= dataInt;
                } else {
                    tmpInts[i] = 0;
                }
            }
            if (changed) {
                for (int i = 0; i < tmpInts.length; i++) {
                    changesInts[i] += tmpInts[i];
                }
            }
        } else if (tmp instanceof char[]) {
            char[] tmpChars = (char[]) tmp;
            for (int i = 0; i < tmpChars.length; i++) {
                char tmpChar = tmpChars[i];
                char dataChar = dataChars[i];
                if (tmpChar != dataChar) {
                    this.changed = true;
                    tmpChars[i] -= dataChar;
                } else {
                    tmpChars[i] = 0;
                }
            }
            if (changed) {
                for (int i = 0; i < tmpChars.length; i++) {
                    changesChars[i] += tmpChars[i];
                }
            }
        } else if (tmp instanceof byte[]) {
            byte[] tmpBytes = (byte[]) tmp;
            for (int i = 0; i < tmpBytes.length; i++) {
                byte tmpByte = tmpBytes[i];
                byte dataByte = dataBytes[i];
                if (tmpByte != dataByte) {
                    this.changed = true;
                    tmpBytes[i] -= dataByte;
                } else {
                    tmpBytes[i] = 0;
                }
            }
            if (changed) {
                for (int i = 0; i < tmpBytes.length; i++) {
                    changesBytes[i] += tmpBytes[i];
                }
            }
        }
        if (caught != null) {
            if (caught instanceof RuntimeException) throw (RuntimeException) caught;
            else throw new RuntimeException(caught);
        }
    }

    @Override
    public void flushChanges(FaweOutputStream out) throws IOException {
        boolean modified = isModified();
        out.writeBoolean(modified);
        if (modified) {
            if (dataBytes != null) {
                out.write(changesBytes);
            } else if (dataInts != null) {
                for (int c : changesInts) {
                    out.writeVarInt(c);
                }
            } else if (dataChars != null) {
                for (char c : changesChars) {
                    out.writeChar(c);
                }
            }
        }
        clearChanges();
    }

    @Override
    public void undoChanges(FaweInputStream in) throws IOException {
        boolean modified = in.readBoolean();
        if (modified) {
            if (dataBytes != null) {
                if (changesBytes != null) {
                    for (int i = 0; i < dataBytes.length; i++) {
                        dataBytes[i] += changesBytes[i];
                    }
                }
                for (int i = 0; i < dataBytes.length; i++) {
                    int read = in.read();
                    dataBytes[i] += read;
                }
            } else if (dataInts != null) {
                if (changesInts != null) {
                    for (int i = 0; i < dataInts.length; i++) {
                        dataInts[i] += changesInts[i];
                    }
                }
                for (int i = 0; i < changesInts.length; i++) {
                    dataInts[i] += in.readVarInt();
                }
            } else if (dataChars != null) {
                if (changesChars != null) {
                    for (int i = 0; i < dataChars.length; i++) {
                        dataChars[i] += changesChars[i];
                    }
                }
                for (int i = 0; i < dataChars.length; i++) {
                    dataChars[i] += in.readChar();
                }
            }
        }
        clearChanges();
    }

    @Override
    public void redoChanges(FaweInputStream in) throws IOException {
        boolean modified = in.readBoolean();
        if (modified) {
            if (dataBytes != null) {
                for (int i = 0; i < dataBytes.length; i++) {
                    int read = in.read();
                    dataBytes[i] -= read;
                }
            } else if (dataInts != null) {
                for (int i = 0; i < dataChars.length; i++) {
                    dataInts[i] -= in.readVarInt();
                }
            } else if (dataChars != null) {
                for (int i = 0; i < dataChars.length; i++) {
                    dataChars[i] -= in.readChar();
                }
            }
        }
        clearChanges();
    }

    public void clearChanges() {
        if (changed) {
            changed = false;
            if (changes != null) {
                if (changesBytes != null) {
                    Arrays.fill(changesBytes, (byte) 0);
                }
                if (changesChars != null) {
                    Arrays.fill(changesChars, (char) 0);
                }
                if (changesInts != null) {
                    Arrays.fill(changesInts, 0);
                }
            }
        }
    }

    public byte[] getByteArray() {
        return dataBytes;
    }

    public char[] getCharArray() {
        return dataChars;
    }

    public int[] getIntArray() {
        return dataInts;
    }

    public boolean isModified() {
        return changed;
    }

    @Override
    public T get() {
        return data;
    }

    public byte getByte(int index) {
        return dataBytes[index];
    }

//    public char getChar(int index) {
//        return dataChars[index];
//    }

    public int getInt(int index) {
        return dataInts[index];
    }

    public void setByte(int index, byte value) {
        changed = true;
        try {
            changesBytes[index] += (dataBytes[index] - value);
        } catch (NullPointerException ignore) {
            changes = (T) (changesBytes = new byte[dataBytes.length]);
            changesBytes[index] += (dataBytes[index] - value);
        }
        dataBytes[index] = value;
    }

    public void setInt(int index, int value) {
        changed = true;
        try {
            changesInts[index] += dataInts[index] - value;
        } catch (NullPointerException ignore) {
            changes = (T) (changesInts = new int[dataInts.length]);
            changesInts[index] += dataInts[index] - value;
        }
        dataInts[index] = value;
    }

//    public void setChar(int index, char value) {
//        changed = true;
//        try {
//            changesChars[index] += dataChars[index] - value;
//        } catch (NullPointerException ignore) {
//            changes = (T) (changesChars = new char[dataChars.length]);
//            changesChars[index] += dataChars[index] - value;
//        }
//        dataChars[index] = value;
//    }
}
