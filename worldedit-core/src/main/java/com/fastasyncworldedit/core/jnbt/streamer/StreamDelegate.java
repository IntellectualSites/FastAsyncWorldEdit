package com.fastasyncworldedit.core.jnbt.streamer;

import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes", "removal"})
public class StreamDelegate {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final byte[][] ZERO_KEYS = new byte[0][];
    private static final StreamDelegate[] ZERO_VALUES = new StreamDelegate[0];

    private Map<String, Tag> retained = null;

    private byte[] buffer;
    private byte[][] keys;
    private StreamDelegate[] values;

    private LazyReader lazyReader;
    private ValueReader elemReader;
    private InfoReader infoReader;
    private ValueReader valueReader;

    private String retainedName = null;
    private String currentName = null;

    /**
     * Used to read a streamed {@link NBTInputStream}
     */
    public StreamDelegate() {
        keys = ZERO_KEYS;
        values = ZERO_VALUES;
    }

    /**
     * Set that keys not added to this StreamDelegate instance should still be retained alongside their value retained. They can
     * be accessed via {@link StreamDelegate#getRetained}
     */
    public StreamDelegate retainOthers() {
        retained = new LinkedHashMap<>();
        return this;
    }

    public StreamDelegate addAndGetParent(String name) {
        add(name);
        return this;
    }

    public StreamDelegate add(@Nullable String name) {
        return add(name, new StreamDelegate());
    }

    private StreamDelegate add(@Nullable String name, StreamDelegate scope) {
        if (valueReader != null) {
            LOGGER.warn(
                    "Scope {} | {} may not run, as the stream is only read once, and a value reader is already set",
                    name,
                    scope
            );
        }
        byte[] bytes = name == null ? new byte[0] : name.getBytes(NBTConstants.CHARSET);
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
            currentName = "";
            retainedName = null;
            return values[0];
        }
        if (nameLength > buffer.length) {
            setRetained(is, nameLength);
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
                    if (key.length < nameLength) {
                        continue;
                    }
                    if (key.length == nameLength) {
                        break;
                    } else {
                        break outer;
                    }
                }
                if (index != keys.length - 1) {
                    int max;
                    for (max = index + 1; max < keys.length; ) {
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
                            currentName = new String(key);
                            retainedName = null;
                            return values[i];
                        }
                        currentName = null;
                        retainedName = new String(Arrays.copyOf(buffer, nameLength), NBTConstants.CHARSET);
                        return null;
                    }
                }
                // fall through
            }
            case 1: {
                byte[] key = keys[index];
                if (key.length == nameLength) {
                    int i = 0;
                    boolean retain = false;
                    for (; nameLength > 0; nameLength--, i++) {
                        byte b = is.readByte();
                        buffer[i] = b;
                        if (!retain && b != key[i]) {
                            if (retained == null) {
                                nameLength--;
                                break outer;
                            }
                            retain = true;
                        }
                    }
                    if (!retain) {
                        currentName = new String(key);
                        retainedName = null;
                        return values[index];
                    }
                    retainedName = new String(Arrays.copyOf(buffer, i), NBTConstants.CHARSET);
                    return null;
                }
                break;
            }
        }
        setRetained(is, nameLength);
        return null;
    }

    private void setRetained(DataInputStream is, int nameLength) throws IOException {
        if (retained == null) {
            is.skipBytes(nameLength);
        } else {
            byte[] nameBytes = new byte[nameLength];
            is.readFully(nameBytes);
            retainedName = new String(nameBytes, NBTConstants.CHARSET);
        }
        currentName = null;
    }

    public StreamDelegate withLong(LongValueReader valueReader) {
        return withElem(valueReader);
    }

    public StreamDelegate withInt(IntValueReader valueReader) {
        return withElem(valueReader);
    }

    public StreamDelegate withValue(ValueReader valueReader) {
        if (keys.length != 0) {
            LOGGER.warn("Reader {} may not run, as the stream is only read once, and a value reader is already set", valueReader);
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
            is.readTagPayloadLazy(type, depth + 1, this);
        }
    }

    public ValueReader getValueReader() {
        return valueReader;
    }

    public ValueReader getElemReader() {
        return elemReader;
    }

    @Nullable
    public Map<String, Tag> getRetained() {
        return retained;
    }

    public void retain(Tag tag) {
        if (retainedName == null) {
            throw new IllegalStateException("Retained name null?!");
        }
        retained.put(retainedName, tag);
        retainedName = null;
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

    public String getCurrentName() {
        return currentName;
    }

    public String getRetainedName() {
        return retainedName;
    }

}
