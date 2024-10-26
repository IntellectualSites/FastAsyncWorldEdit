/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.jnbt;

import com.fastasyncworldedit.core.jnbt.streamer.StreamDelegate;
import com.fastasyncworldedit.core.jnbt.streamer.ValueReader;
import org.enginehub.linbus.stream.LinBinaryIO;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class reads <strong>NBT</strong>, or <strong>Named Binary Tag</strong>
 * streams, and produces an object graph of subclasses of the {@code Tag}
 * object.
 *
 * <p>
 * The NBT format was created by Markus Persson, and the specification may be
 * found at <a href="https://minecraft.gamepedia.com/NBT_format">
 * https://minecraft.gamepedia.com/NBT_format</a>.
 * </p>
 *
 * @deprecated JNBT is being removed for lin-bus in WorldEdit 8, use {@link LinBinaryIO} instead
 */
@SuppressWarnings("removal")
@Deprecated(forRemoval = true)
public final class NBTInputStream implements Closeable {

    private final DataInputStream is;

    /**
     * Creates a new {@code NBTInputStream}, which will source its data
     * from the specified input stream.
     *
     * @param is the input stream
     */
    public NBTInputStream(InputStream is) {
        this.is = new DataInputStream(is);
    }

    //FAWE start
    public NBTInputStream(DataInputStream dis) {
        this.is = dis;
    }

    public void mark(int mark) {
        is.mark(mark);
    }

    public void reset() throws IOException {
        is.reset();
    }

    //FAWE end
    /**
     * Reads an NBT tag from the stream.
     *
     * @return The tag that was read.
     */
    public NamedTag readNamedTag() throws IOException {
        //FAWE start
        return readNamedTag(0);
    }

    /**
     * Reads an NBT from the stream.
     *
     * @param depth the depth of this tag
     * @return The tag that was read.
     * @throws IOException if an I/O error occurs.
     */
    private NamedTag readNamedTag(int depth) throws IOException {
        int type = is.readByte() & 0xFF;
        return new NamedTag(readNamedTagName(type), readTagPayload(type, depth));
    }

    public Tag readTag() throws IOException {
        int type = is.readByte();
        return readTagPayload(type, 0);
    }

    public void readNamedTagLazy(StreamDelegate scope) {
        try {
            int type = is.readByte();
            if (type == NBTConstants.TYPE_END) {
                return;
            }

            StreamDelegate child = scope.get(is);
            if (child != null) {
                child.acceptRoot(this, type, 0);
            } else {
                readTagPayloadLazy(type, 0, scope, scope.getRetained() != null);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void readNamedTagLazyExceptionally(StreamDelegate scope) throws IOException {
        int type = is.readByte();
        if (type == NBTConstants.TYPE_END) {
            return;
        }

        StreamDelegate child = scope.get(is);
        if (child != null) {
            child.acceptRoot(this, type, 0);
        } else {
            readTagPayloadLazy(type, 0, scope, scope.getRetained() != null);
        }
    }

    public String readNamedTagName(int type) throws IOException {
        if (type != NBTConstants.TYPE_END) {
            return is.readUTF();
        } else {
            return "";
        }
    }

    private byte[] buf;

    public void readTagPayloadLazy(int type, int depth) throws IOException {
        readTagPayloadLazy(type, depth, null, false);
    }

    public void readTagPayloadLazy(int type, int depth, StreamDelegate scope, boolean retain) throws IOException {
        int length;
        switch (type) {
            case NBTConstants.TYPE_END -> {
            }
            case NBTConstants.TYPE_BYTE -> {
                if (!retain) {
                    is.skipBytes(1);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_SHORT -> {
                if (!retain) {
                    is.skipBytes(2);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_INT, NBTConstants.TYPE_FLOAT -> {
                if (!retain) {
                    is.skipBytes(4);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_LONG, NBTConstants.TYPE_DOUBLE -> {
                if (!retain) {
                    is.skipBytes(8);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_STRING -> {
                length = is.readShort() & 0xFFFF;
                if (!retain) {
                    is.skipBytes(length);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_BYTE_ARRAY -> {
                if (!retain) {
                    is.skipBytes(is.readInt());
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_LIST -> {
                if (!retain) {
                    int childType = is.readByte();
                    length = is.readInt();
                    for (int i = 0; i < length; ++i) {
                        readTagPayloadLazy(childType, depth + 1, scope, retain);
                    }
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_COMPOUND -> {
                if (!retain) {
                    // readDataPayload
                    depth++;
                    while (true) {
                        int childType = is.readByte();
                        if (childType == NBTConstants.TYPE_END) {
                            return;
                        }
                        is.skipBytes(is.readShort() & 0xFFFF);
                        readTagPayloadLazy(childType, depth + 1, scope, retain);
                    }
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_INT_ARRAY -> {
                if (!retain) {
                    is.skipBytes(is.readInt() << 2);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            case NBTConstants.TYPE_LONG_ARRAY -> {
                if (!retain) {
                    is.skipBytes(is.readInt() << 3);
                } else {
                    scope.retain(readTagPayload(type, depth));
                }
            }
            default -> throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    public void readTagPayloadLazy(int type, int depth, StreamDelegate scope) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END -> {
            }
            case NBTConstants.TYPE_BYTE -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyInt(0, is.readByte());
                } else {
                    is.skipBytes(1);
                }
            }
            case NBTConstants.TYPE_SHORT -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyInt(0, is.readShort());
                } else {
                    is.skipBytes(2);
                }
            }
            case NBTConstants.TYPE_INT -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyInt(0, is.readInt());
                } else {
                    is.skipBytes(4);
                }
            }
            case NBTConstants.TYPE_LONG -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyLong(0, is.readLong());
                } else {
                    is.skipBytes(8);
                }
            }
            case NBTConstants.TYPE_FLOAT -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyFloat(0, is.readFloat());
                } else {
                    is.skipBytes(4);
                }
            }
            case NBTConstants.TYPE_DOUBLE -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.applyDouble(0, is.readDouble());
                } else {
                    is.skipBytes(8);
                }
            }
            case NBTConstants.TYPE_STRING -> {
                ValueReader value = scope.getValueReader();
                if (value == null) {
                    value = scope.getElemReader();
                }
                if (value != null) {
                    value.apply(0, is.readUTF());
                } else {
                    int length = is.readShort() & 0xFFFF;
                    is.skipBytes(length);
                }
            }
            case NBTConstants.TYPE_LIST -> {
                int childType = is.readByte();
                int length = is.readInt();
                StreamDelegate child;
                scope.acceptInfo(length, childType);
                ValueReader valueReader = scope.getValueReader();
                if (valueReader != null) {
                    List<Object> tagList = readListRaw(depth, childType, length);
                    valueReader.apply(0, tagList);
                    return;
                }
                valueReader = scope.getElemReader();
                if (valueReader != null) {
                    for (int i = 0; i < length; ++i) {
                        valueReader.apply(i, readTagPayloadRaw(childType, depth + 1));
                    }
                    return;
                }
                child = scope.get0();
                if (child == null) {
                    for (int i = 0; i < length; ++i) {
                        readTagPayloadLazy(childType, depth + 1, scope, scope.getRetained() != null);
                    }
                } else {
                    for (int i = 0; i < length; ++i) {
                        readTagPayloadLazy(childType, depth + 1, child);
                    }
                }
            }
            case NBTConstants.TYPE_COMPOUND -> {
                // readDataPayload
                scope.acceptInfo(-1, NBTConstants.TYPE_BYTE);
                ValueReader valueReader = scope.getValueReader();
                if (valueReader != null) {
                    valueReader.apply(0, this.readTagPayloadRaw(type, depth));
                    return;
                }
                valueReader = scope.getElemReader();
                if (valueReader != null) {
                    for (int i = 0; ; i++) {
                        int childType = is.readByte();
                        if (childType == NBTConstants.TYPE_END) {
                            return;
                        }
                        String key = readNamedTagName(childType);
                        Object value = readTagPayloadRaw(childType, depth + 1);
                        AbstractMap.SimpleEntry<String, Object> entry = new AbstractMap.SimpleEntry<>(key, value);
                        valueReader.apply(i, entry);
                    }
                }
                while (true) {
                    int childType = is.readByte();
                    if (childType == NBTConstants.TYPE_END) {
                        return;
                    }
                    StreamDelegate child = scope.get(is);
                    try {
                        if (child == null) {
                            readTagPayloadLazy(childType, depth + 1, scope, scope.getRetained() != null);
                        } else {
                            readTagPayloadLazy(childType, depth + 1, child);
                        }
                    } catch (IOException e) {
                        String cur = scope.getCurrentName() == null ? scope.getRetainedName() : scope.getCurrentName();
                        if (cur != null) {
                            throw new IOException("Error reading child scope: `" + scope.getCurrentName() + "`", e);
                        } else {
                            throw e;
                        }
                    }
                }
            }
            case NBTConstants.TYPE_BYTE_ARRAY -> {
                int length = is.readInt();
                scope.acceptInfo(length, NBTConstants.TYPE_BYTE);
                if (scope.acceptLazy(length, this)) {
                    return;
                }
                ValueReader valueReader = scope.getValueReader();
                if (valueReader != null) {
                    byte[] arr = new byte[length];
                    is.readFully(arr);
                    valueReader.apply(0, arr);
                    return;
                }
                valueReader = scope.getElemReader();
                if (valueReader != null) {
                    int i = 0;
                    DataInputStream dis = is;
                    if (length > 1024) {
                        if (buf == null) {
                            buf = new byte[1024];
                        }
                        int left = length;
                        for (; left > 1024; left -= 1024) {
                            dis.readFully(buf);
                            for (byte b : buf) {
                                valueReader.applyInt(i++, b & 0xFF);
                            }
                        }
                    }
                    for (; i < length; i++) {
                        valueReader.applyInt(i, dis.read());
                    }
                    return;
                }
                is.skipBytes(length);
            }
            case NBTConstants.TYPE_INT_ARRAY -> {
                int length = is.readInt();
                scope.acceptInfo(length, NBTConstants.TYPE_INT);
                if (scope.acceptLazy(length, this)) {
                    return;
                }
                ValueReader valueReader = scope.getValueReader();
                if (valueReader != null) {
                    valueReader.apply(0, readIntArrayRaw(length));
                    return;
                }
                valueReader = scope.getElemReader();
                if (valueReader != null) {
                    for (int i = 0; i < length; i++) {
                        valueReader.applyInt(i, is.readInt());
                    }
                    return;
                }
                is.skipBytes(length << 2);
            }
            case NBTConstants.TYPE_LONG_ARRAY -> {
                int length = is.readInt();
                scope.acceptInfo(length, NBTConstants.TYPE_LONG);
                if (scope.acceptLazy(length, this)) {
                    return;
                }
                ValueReader valueReader = scope.getValueReader();
                if (valueReader != null) {
                    valueReader.apply(0, readLongArrayRaw(length));
                    return;
                }
                valueReader = scope.getElemReader();
                if (valueReader != null) {
                    for (int i = 0; i < length; i++) {
                        valueReader.applyLong(i, is.readLong());
                    }
                    return;
                }
                is.skipBytes(length << 3);
            }
            default -> throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    private List<Object> readListRaw(int depth, int childType, int length) throws IOException {
        List<Object> tagList = new ArrayList<>(length);
        for (int i = 0; i < length; ++i) {
            Object tag = readTagPayloadRaw(childType, depth + 1);
            if (tag == null) {
                throw new IOException("TAG_End not permitted in a list.");
            }
            tagList.add(tag);
        }
        return tagList;
    }

    public static int getSize(int type) {
        return switch (type) {
            default -> 1;
            case NBTConstants.TYPE_BYTE_ARRAY, NBTConstants.TYPE_STRING, NBTConstants.TYPE_LIST, NBTConstants.TYPE_COMPOUND, NBTConstants.TYPE_INT_ARRAY, NBTConstants.TYPE_LONG_ARRAY, NBTConstants.TYPE_SHORT ->
                    2;
            case NBTConstants.TYPE_FLOAT, NBTConstants.TYPE_INT -> 4;
            case NBTConstants.TYPE_DOUBLE, NBTConstants.TYPE_LONG -> 8;
        };
    }

    public Object readTagPayloadRaw(int type, int depth) throws IOException {
        int length;
        byte[] bytes;
        switch (type) {
            case NBTConstants.TYPE_END -> {
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return null;
                }
            }
            case NBTConstants.TYPE_BYTE -> {
                return (is.readByte());
            }
            case NBTConstants.TYPE_SHORT -> {
                return (is.readShort());
            }
            case NBTConstants.TYPE_INT -> {
                return (is.readInt());
            }
            case NBTConstants.TYPE_LONG -> {
                return (is.readLong());
            }
            case NBTConstants.TYPE_FLOAT -> {
                return (is.readFloat());
            }
            case NBTConstants.TYPE_DOUBLE -> {
                return (is.readDouble());
            }
            case NBTConstants.TYPE_BYTE_ARRAY -> {
                length = is.readInt();
                bytes = new byte[length];
                is.readFully(bytes);
                return (bytes);
            }
            case NBTConstants.TYPE_STRING -> {
                return is.readUTF();
            }
            case NBTConstants.TYPE_LIST -> {
                int childType = is.readByte();
                length = is.readInt();
                return readListRaw(depth, childType, length);
            }
            case NBTConstants.TYPE_COMPOUND -> {
                Map<String, Object> tagMap = new HashMap<>();
                while (true) {
                    int childType = is.readByte();
                    if (childType == NBTConstants.TYPE_END) {
                        return tagMap;
                    }
                    String name = readNamedTagName(childType);
                    Object value = readTagPayloadRaw(childType, depth + 1);
                    tagMap.put(name, value);
                }
            }
            case NBTConstants.TYPE_INT_ARRAY -> {
                length = is.readInt();
                return readIntArrayRaw(length);
            }
            case NBTConstants.TYPE_LONG_ARRAY -> {
                length = is.readInt();
                return readLongArrayRaw(length);
            }
            default -> throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    private int[] readIntArrayRaw(int length) throws IOException {
        int[] data = new int[length];
        if (buf == null) {
            buf = new byte[1024];
        }
        int index = 0;
        while (length > 0) {
            int toRead = Math.min(length << 2, buf.length);
            is.readFully(buf, 0, toRead);
            for (int i = 0; i < toRead; i += 4, index++) {
                data[index] = ((buf[i] & 0xFF) << 24) + ((buf[i + 1] & 0xFF) << 16) + ((buf[i + 2] & 0xFF) << 8) + (buf[i + 3] & 0xFF);
            }
            length -= (toRead >> 2);
        }
        return data;
    }

    private long[] readLongArrayRaw(int length) throws IOException {
        long[] data = new long[length];
        if (buf == null) {
            buf = new byte[1024];
        }
        int index = 0;
        while (length > 0) {
            int toRead = Math.min(length << 3, buf.length);
            is.readFully(buf, 0, toRead);
            for (int i = 0; i < toRead; i += 8, index++) {
                data[index] = (((long) buf[i] << 56) | ((long) (buf[i + 1] & 255) << 48) | ((long) (buf[i + 2] & 255) << 40) | ((long) (buf[i + 3] & 255) << 32) | ((long) (buf[i + 4] & 255) << 24) | ((buf[i + 5] & 255) << 16) | ((buf[i + 6] & 255) << 8) | (buf[i + 7] & 255));
            }
            length -= (toRead >> 3);
        }
        return (data);
    }

    /**
     * Reads the payload of a tag given the type.
     *
     * @param type  the type
     * @param depth the depth
     * @return the tag
     * @throws IOException if an I/O error occurs.
     */
    public Tag readTagPayload(int type, int depth) throws IOException { //FAWE - public
        int length;
        byte[] bytes;
        switch (type) {
            case NBTConstants.TYPE_END -> {
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return new EndTag();
                }
            }
            case NBTConstants.TYPE_BYTE -> {
                return new ByteTag(is.readByte());
            }
            case NBTConstants.TYPE_SHORT -> {
                return new ShortTag(is.readShort());
            }
            case NBTConstants.TYPE_INT -> {
                return new IntTag(is.readInt());
            }
            case NBTConstants.TYPE_LONG -> {
                return new LongTag(is.readLong());
            }
            case NBTConstants.TYPE_FLOAT -> {
                return new FloatTag(is.readFloat());
            }
            case NBTConstants.TYPE_DOUBLE -> {
                return new DoubleTag(is.readDouble());
            }
            case NBTConstants.TYPE_BYTE_ARRAY -> {
                length = is.readInt();
                bytes = new byte[length];
                is.readFully(bytes);
                return new ByteArrayTag(bytes);
            }
            case NBTConstants.TYPE_STRING -> {
                return new StringTag(is.readUTF());
            }
            case NBTConstants.TYPE_LIST -> {
                int childType = is.readByte();
                length = is.readInt();
                List<Tag> tagList = new ArrayList<>();
                for (int i = 0; i < length; ++i) {
                    Tag tag = readTagPayload(childType, depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    tagList.add(tag);
                }
                return new ListTag(NBTUtils.getTypeClass(childType), tagList);
            }
            case NBTConstants.TYPE_COMPOUND -> {
                Map<String, Tag<?, ?>> tagMap = new HashMap<>();
                while (true) {
                    NamedTag namedTag = readNamedTag(depth + 1);
                    Tag tag = namedTag.getTag();
                    if (tag instanceof EndTag) {
                        break;
                    } else {
                        tagMap.put(namedTag.getName(), tag);
                    }
                }
                return new CompoundTag(tagMap);
            }
            case NBTConstants.TYPE_INT_ARRAY -> {
                length = is.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readInt();
                }
                return new IntArrayTag(data);
            }
            case NBTConstants.TYPE_LONG_ARRAY -> {
                length = is.readInt();
                long[] longData = new long[length];
                for (int i = 0; i < length; i++) {
                    longData[i] = is.readLong();
                }
                return new LongArrayTag(longData);
            }
            default -> throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    //FAWE start - Copied from FaweStreamChangeSet
    public Iterator<CompoundTag> toIterator() {
        return new Iterator<CompoundTag>() {
            private CompoundTag last = read();

            public CompoundTag read() {
                try {
                    return (CompoundTag) NBTInputStream.this.readTag();
                } catch (Exception ignored) {
                    // Assume input is complete
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public CompoundTag next() {
                CompoundTag tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }
    //FAWE end

}
