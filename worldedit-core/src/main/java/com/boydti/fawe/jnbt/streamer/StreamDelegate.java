package com.boydti.fawe.jnbt.streamer;

import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;

import java.io.DataInputStream;
import java.io.IOException;

public class StreamDelegate {
    private static final byte[][] ZERO_KEYS = new byte[0][];
    private static final StreamDelegate[] ZERO_VALUES = new StreamDelegate[0];

    private byte[] buffer;
    private byte[][] keys;
    private StreamDelegate[] values;

    private LazyReader lazyReader;
    private ValueReader elemReader;
    private InfoReader infoReader;
    private ValueReader valueReader;

    public StreamDelegate() {
        keys = ZERO_KEYS;
        values = ZERO_VALUES;
    }

    public StreamDelegate addAndGetParent(String name) {
        add(name);
        return this;
    }

    public StreamDelegate add() {
        return add("");
    }

    public StreamDelegate add(String name) {
        return add(name, new StreamDelegate());
    }

    private StreamDelegate add(String name, StreamDelegate scope) {
        if (valueReader != null) {
            System.out.println("Scope " + name + " | " + scope + " may not run, as the stream is only read once, and a value reader is already set");
        }
        byte[] bytes = name.getBytes(NBTConstants.CHARSET);
        int maxSize = bytes.length;

        byte[][] tmpKeys = new byte[keys.length + 1][];
        StreamDelegate[] tmpValues = new StreamDelegate[keys.length + 1];
        tmpKeys[keys.length] = bytes;
        tmpValues[keys.length] = scope;

        int i = 0;
        for (; i < keys.length; i++) {
            byte[] key = keys[i];
            if (key.length >= bytes.length) {
                tmpKeys[i] = bytes;
                tmpValues[i] = scope;
                break;
            }
            tmpKeys[i] = key;
            tmpValues[i] = values[i];
            maxSize = Math.max(maxSize, key.length);
        }
        for (; i < keys.length; i++) {
            byte[] key = keys[i];
            tmpKeys[i + 1] = key;
            tmpValues[i + 1] = values[i];
            maxSize = Math.max(maxSize, key.length);
        }

        this.keys = tmpKeys;
        this.values = tmpValues;
        if (this.buffer == null || buffer.length < maxSize) {
            buffer = new byte[maxSize];
        }
        return scope;
    }

    public StreamDelegate get0() {
        if (keys.length > 0 && keys[0].length == 0) {
            return values[0];
        }
        return null;
    }

    public StreamDelegate get(DataInputStream is) throws IOException {
        int nameLength = is.readShort() & 0xFFFF;
        if (nameLength == 0 && keys.length > 0 && keys[0].length == 0) {
            return values[0];
        }
        if (nameLength > buffer.length) {
            is.skipBytes(nameLength);
            return null;
        }
        int index = 0;
        outer:
        switch (keys.length) {
            case 0:
                break;
            default: {
                for (; index < keys.length; index++) {
                    byte[] key = keys[index];
                    if (key.length < nameLength) continue;
                    if (key.length == nameLength) {
                        break;
                    } else {
                        break outer;
                    }
                }
                if (index != keys.length - 1) {
                    int max;
                    for (max = index + 1; max < keys.length;) {
                        byte[] key = keys[max];
                        if (key.length == nameLength) {
                            max++;
                            continue;
                        }
                        break;
                    }
                    if (index != max) {
                        is.readFully(buffer, 0, nameLength);
                        middle:
                        for (int i = index; i < max; i++) {
                            byte[] key = keys[i];
                            for (int j = 0; j < nameLength; j++) {
                                if (buffer[j] != key[j]) {
                                    continue middle;
                                }
                            }
                            return values[i];
                        }
                        return null;
                    }
                }
            }
            case 1: {
                byte[] key = keys[index];
                if (key.length == nameLength) {
                    int i = 0;
                    for (; nameLength > 0; nameLength--, i++) {
                        byte b = is.readByte();
                        buffer[i] = b;
                        if (b != key[i]) {
                            nameLength--;
                            break outer;
                        }

                    }
                    return values[index];
                }
                break;
            }
        }
        is.skipBytes(nameLength);
        return null;
    }

    public StreamDelegate withLong(LongValueReader valueReader) {
        return withElem(valueReader);
    }

    public StreamDelegate withInt(IntValueReader valueReader) {
        return withElem(valueReader);
    }

    public StreamDelegate withValue(ValueReader valueReader) {
        if (keys.length != 0) {
            System.out.println("Reader " + valueReader + " may not run, as the stream is only read once, and a value reader is already set");
        }
        this.valueReader = valueReader;
        return this;
    }

    public StreamDelegate withStream(LazyReader lazyReader) {
        this.lazyReader = lazyReader;
        return this;
    }

    public StreamDelegate withElem(ValueReader elemReader) {
        this.elemReader = elemReader;
        return this;
    }

    public StreamDelegate withInfo(InfoReader infoReader) {
        this.infoReader = infoReader;
        return this;
    }

    public void acceptRoot(NBTInputStream is, int type, int depth) throws IOException {
        if (lazyReader != null) {
            lazyReader.apply(0, is);
        } else if (elemReader != null) {
            Object raw = is.readTagPayloadRaw(type, depth);
            elemReader.apply(0, raw);
        } else if (valueReader != null) {
            Object raw = is.readTagPayloadRaw(type, depth);
            valueReader.apply(0, raw);
        } else {
            is.readTagPaylodLazy(type, depth + 1, this);
        }
    }

    public ValueReader getValueReader() {
        return valueReader;
    }

    public ValueReader getElemReader() {
        return elemReader;
    }

    public void acceptInfo(int length, int type) throws IOException {
        if (infoReader != null) {
            infoReader.apply(length, type);
        }
    }

    public boolean acceptLazy(int length, NBTInputStream is) throws IOException {
        if (lazyReader != null) {
            lazyReader.apply(length, is);
            return true;
        }
        return false;
    }
}
