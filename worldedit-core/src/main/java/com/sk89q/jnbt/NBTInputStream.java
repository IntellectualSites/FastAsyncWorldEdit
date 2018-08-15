/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.jnbt;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.RunnableVal2;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class reads <strong>NBT</strong>, or <strong>Named Binary Tag</strong>
 * streams, and produces an object graph of subclasses of the {@code Tag}
 * object.
 * <p>
 * <p>The NBT format was created by Markus Persson, and the specification may be
 * found at <a href="http://www.minecraft.net/docs/NBT.txt">
 * http://www.minecraft.net/docs/NBT.txt</a>.</p>
 */
public final class NBTInputStream implements Closeable {

    private final DataInput is;

    /**
     * Creates a new {@code NBTInputStream}, which will source its data
     * from the specified input stream.
     *
     * @param is the input stream
     * @throws IOException if an I/O error occurs
     */
    public NBTInputStream(InputStream is) throws IOException {
        this.is = new DataInputStream(is);
    }

    public NBTInputStream(DataInputStream dis) {
        this.is = dis;
    }

    public NBTInputStream(DataInput di) {
        this.is = di;
    }

    /**
     * Reads an NBT tag from the stream.
     *
     * @return The tag that was read.
     * @throws IOException if an I/O error occurs.
     */
    public NamedTag readNamedTag() throws IOException {
        return readNamedTag(0);
    }

    /**
     * Reads an NBT map from the stream.
     *
     * @return The map that was read.
     * @throws IOException if an I/O error occurs.
     */
    public NamedData readNamedData() throws IOException {
        return readNamedData(0);
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

    private NamedData readNamedData(int depth) throws IOException {
        int type = is.readByte();
        return new NamedData(readNamedTagName(type), readDataPayload(type, depth));
    }

    public Tag readTag() throws IOException {
        int type = is.readByte();
        return readTagPayload(type, 0);
    }

    public Object readData() throws IOException {
        int type = is.readByte();
        return readDataPayload(type, 0);
    }

    public void readNamedTagLazy(Function<String, BiConsumer> getReader) throws IOException {
        int type = is.readByte();
        String name = readNamedTagName(type);
        BiConsumer reader = getReader.apply(name);
        if (reader != null) {
            reader.accept(0, readTagPaylodRaw(type, 0));
            return;
        }
        readTagPaylodLazy(type, 0, name, getReader);
    }

    public String readNamedTagName(int type) throws IOException {
        String name;
        if (type != NBTConstants.TYPE_END) {
            int nameLength = is.readShort() & 0xFFFF;
            byte[] nameBytes = new byte[nameLength];
            is.readFully(nameBytes);
            return new String(nameBytes, NBTConstants.CHARSET);
        } else {
            return "";
        }
    }

    private byte[] buf;

    public void readTagPaylodLazy(int type, int depth, String node, Function<String, BiConsumer> getReader) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                return;
            case NBTConstants.TYPE_BYTE:
                is.skipBytes(1);
                return;
            case NBTConstants.TYPE_SHORT:
                is.skipBytes(2);
                return;
            case NBTConstants.TYPE_INT:
                is.skipBytes(4);
                return;
            case NBTConstants.TYPE_LONG:
                is.skipBytes(8);
                return;
            case NBTConstants.TYPE_FLOAT:
                is.skipBytes(4);
                return;
            case NBTConstants.TYPE_DOUBLE:
                is.skipBytes(8);
                return;
            case NBTConstants.TYPE_STRING:
                int length = is.readShort();
                is.skipBytes(length);
                return;
            case NBTConstants.TYPE_BYTE_ARRAY:
                BiConsumer reader = getReader.apply(node + ".?");
                length = is.readInt();
                if (reader != null) {
                    reader.accept(length, NBTConstants.TYPE_BYTE);
                }
                reader = getReader.apply(node + ".#");
                if (reader == null) {
                    is.skipBytes(length);
                    return;
                }
                if (reader instanceof NBTStreamer.ByteReader) {
                    NBTStreamer.ByteReader byteReader = (NBTStreamer.ByteReader) reader;
                    int i = 0;
                    if (is instanceof InputStream) {
                        DataInputStream dis = (DataInputStream) is;
                        if (length > 1024) {
                            if (buf == null) {
                                buf = new byte[1024];
                            }
                            int left = length;
                            for (; left > 1024; left -= 1024) {
                                dis.readFully(buf);
                                for (byte b : buf) {
                                    byteReader.run(i++, b & 0xFF);
                                }
                            }
                        }
                        for (; i < length; i++) {
                            byteReader.run(i, dis.read());
                        }
                    } else {
                        if (length > 1024) {
                            if (buf == null) {
                                buf = new byte[1024];
                            }
                            int left = length;
                            for (; left > 1024; left -= 1024) {
                                is.readFully(buf);
                                for (byte b : buf) {
                                    byteReader.run(i++, b & 0xFF);
                                }
                            }
                        }
                        for (; i < length; i++) {
                            byteReader.run(i, is.readByte() & 0xFF);
                        }
                    }
                } else {
                    for (int i = 0; i < length; i++) {
                        reader.accept(i, is.readByte());
                    }
                }
                return;
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                if (childType == NBTConstants.TYPE_LIST) {
                    childType = NBTConstants.TYPE_COMPOUND;
                }
                length = is.readInt();
                reader = getReader.apply(node + ".?");
                if (reader != null) {
                    reader.accept(length, childType);
                }
                node += ".#";
                reader = getReader.apply(node);
                depth++;
                if (reader == null) {
                    for (int i = 0; i < length; ++i) {
                        readTagPaylodLazy(childType, depth, node, getReader);
                    }
                    return;
                }
                for (int i = 0; i < length; ++i) {
                    reader.accept(i, readTagPayload(childType, depth));
                }
                return;
            case NBTConstants.TYPE_COMPOUND:
                depth++;
                // 3
                for (int i = 0; ; i++) {
                    childType = is.readByte();
                    if (childType == NBTConstants.TYPE_END) {
                        return;
                    }
                    String name = readNamedTagName(childType);
                    String childNode = node + "." + name;
                    reader = getReader.apply(childNode);
                    if (reader == null) {
                        readTagPaylodLazy(childType, depth, childNode, getReader);
                        continue;
                    }
                    reader.accept(i, readTagPaylodRaw(childType, depth));
                }
            case NBTConstants.TYPE_INT_ARRAY: {
                length = is.readInt();
                reader = getReader.apply(node + ".?");
                if (reader != null) {
                    reader.accept(length, NBTConstants.TYPE_INT);
                }
                reader = getReader.apply(node + ".#");
                if (reader == null) {
                    is.skipBytes(length << 2);
                    return;
                }
                if (reader instanceof NBTStreamer.ByteReader) {
                    NBTStreamer.ByteReader byteReader = (NBTStreamer.ByteReader) reader;
                    for (int i = 0; i < length; i++) {
                        byteReader.run(i, is.readInt());
                    }
                } else {
                    for (int i = 0; i < length; i++) {
                        reader.accept(i, is.readInt());
                    }
                }
                return;
            }
            case NBTConstants.TYPE_LONG_ARRAY: {
                length = is.readInt();
                reader = getReader.apply(node + ".?");
                if (reader != null) {
                    reader.accept(length, NBTConstants.TYPE_LONG);
                }
                reader = getReader.apply(node + ".#");
                if (reader == null) {
                    is.skipBytes(length << 3);
                    return;
                }
                if (reader instanceof NBTStreamer.LongReader) {
                    NBTStreamer.LongReader longReader = (NBTStreamer.LongReader) reader;
                    for (int i = 0; i < length; i++) {
                        longReader.run(i, is.readLong());
                    }
                } else {
                    for (int i = 0; i < length; i++) {
                        reader.accept(i, is.readLong());
                    }
                }
                return;
            }

            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    public static int getSize(int type) {
        switch (type) {
            default:
            case NBTConstants.TYPE_END:
            case NBTConstants.TYPE_BYTE:
                return 1;
            case NBTConstants.TYPE_BYTE_ARRAY:
            case NBTConstants.TYPE_STRING:
            case NBTConstants.TYPE_LIST:
            case NBTConstants.TYPE_COMPOUND:
            case NBTConstants.TYPE_INT_ARRAY:
            case NBTConstants.TYPE_LONG_ARRAY:
            case NBTConstants.TYPE_SHORT:
                return 2;
            case NBTConstants.TYPE_FLOAT:
            case NBTConstants.TYPE_INT:
                return 4;
            case NBTConstants.TYPE_DOUBLE:
            case NBTConstants.TYPE_LONG:
                return 8;
        }
    }

    private Object readTagPaylodRaw(int type, int depth) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return null;
                }
            case NBTConstants.TYPE_BYTE:
                return (is.readByte());
            case NBTConstants.TYPE_SHORT:
                return (is.readShort());
            case NBTConstants.TYPE_INT:
                return (is.readInt());
            case NBTConstants.TYPE_LONG:
                return (is.readLong());
            case NBTConstants.TYPE_FLOAT:
                return (is.readFloat());
            case NBTConstants.TYPE_DOUBLE:
                return (is.readDouble());
            case NBTConstants.TYPE_BYTE_ARRAY:
                int length = is.readInt();
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return (bytes);
            case NBTConstants.TYPE_STRING:
                length = is.readShort();
                bytes = new byte[length];
                is.readFully(bytes);
                return (new String(bytes, NBTConstants.CHARSET));
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                if (childType == NBTConstants.TYPE_LIST) {
                    childType = NBTConstants.TYPE_COMPOUND;
                }
                length = is.readInt();
                List<Tag> tagList = new ArrayList<Tag>();
                for (int i = 0; i < length; ++i) {
                    Tag tag = readTagPayload(childType, depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    tagList.add(tag);
                }
                return (tagList);
            case NBTConstants.TYPE_COMPOUND:
                Map<String, Tag> tagMap = new HashMap<String, Tag>();
                while (true) {
                    NamedTag namedTag = readNamedTag(depth + 1);
                    Tag tag = namedTag.getTag();
                    if (tag instanceof EndTag) {
                        break;
                    } else {
                        tagMap.put(namedTag.getName(), tag);
                    }
                }
                return (tagMap);
            case NBTConstants.TYPE_INT_ARRAY: {
                length = is.readInt();
                int[] data = new int[length];
                if (buf == null) {
                    buf = new byte[1024];
                }
                int index = 0;
                while (length > 0) {
                    int toRead = Math.min(length << 2, buf.length);
                    is.readFully(buf, 0, toRead);
                    for (int i = 0; i < toRead; i += 4, index++) {
                        data[index] = ((buf[i] << 24) + (buf[i + 1] << 16) + (buf[i + 2] << 8) + (buf[i + 3]));
                    }
                    length -= toRead;
                }
                return (data);
            }
            case NBTConstants.TYPE_LONG_ARRAY: {
                length = is.readInt();
                long[] data = new long[length];
                if (buf == null) {
                    buf = new byte[1024];
                }
                int index = 0;
                while (length > 0) {
                    int toRead = Math.min(length << 3, buf.length);
                    is.readFully(buf, 0, toRead);
                    for (int i = 0; i < toRead; i += 8, index++) {
                        data[index] = (((long) buf[i] << 56) | ((long) buf[i + 1] << 48) | ((long) buf[i + 2] << 40) | ((long) buf[i + 3] << 32) | (buf[i + 4] << 24) | (buf[i + 5] << 16) | (buf[i + 6] << 8) | (buf[i + 7]));
                    }
                    length -= toRead;
                }
                return (data);
            }
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    public Object readDataPayload(int type, int depth) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return null;
                }
            case NBTConstants.TYPE_BYTE:
                return is.readByte();
            case NBTConstants.TYPE_SHORT:
                return is.readShort();
            case NBTConstants.TYPE_INT:
                return is.readInt();
            case NBTConstants.TYPE_LONG:
                return is.readLong();
            case NBTConstants.TYPE_FLOAT:
                return is.readFloat();
            case NBTConstants.TYPE_DOUBLE:
                return is.readDouble();
            case NBTConstants.TYPE_BYTE_ARRAY:
                int length = is.readInt();
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return bytes;
            case NBTConstants.TYPE_STRING:
                length = is.readShort();
                bytes = new byte[length];
                is.readFully(bytes);
                return new String(bytes, NBTConstants.CHARSET);
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                if (childType == NBTConstants.TYPE_LIST) {
                    childType = NBTConstants.TYPE_COMPOUND;
                }
                length = is.readInt();
                ArrayList<Object> list = new ArrayList<>();
                for (int i = 0; i < length; ++i) {
                    Object obj = readDataPayload(childType, depth + 1);
                    if (obj == null) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    list.add(obj);
                }

                return list;
            case NBTConstants.TYPE_COMPOUND:
                Map<String, Object> map = new HashMap<>();
                while (true) {
                    int newType = is.readByte();
                    String name = readNamedTagName(newType);
                    Object data = readDataPayload(newType, depth + 1);
                    if (data == null) {
                        break;
                    } else {
                        map.put(name, data);
                    }
                }

                return map;
            case NBTConstants.TYPE_INT_ARRAY: {
                length = is.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readInt();
                }
                return data;
            }
            case NBTConstants.TYPE_LONG_ARRAY: {
                length = is.readInt();
                long[] data = new long[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readLong();
                }
                return data;
            }
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    /**
     * Reads the payload of a tag given the type.
     *
     * @param type  the type
     * @param depth the depth
     * @return the tag
     * @throws IOException if an I/O error occurs.
     */
    public Tag readTagPayload(int type, int depth) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return EndTag.INSTANCE;
                }
            case NBTConstants.TYPE_BYTE:
                return new ByteTag(is.readByte());
            case NBTConstants.TYPE_SHORT:
                return new ShortTag(is.readShort());
            case NBTConstants.TYPE_INT:
                return new IntTag(is.readInt());
            case NBTConstants.TYPE_LONG:
                return new LongTag(is.readLong());
            case NBTConstants.TYPE_FLOAT:
                return new FloatTag(is.readFloat());
            case NBTConstants.TYPE_DOUBLE:
                return new DoubleTag(is.readDouble());
            case NBTConstants.TYPE_BYTE_ARRAY:
                int length = is.readInt();
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return new ByteArrayTag(bytes);
            case NBTConstants.TYPE_STRING:
                length = is.readShort();
                bytes = new byte[length];
                is.readFully(bytes);
                return new StringTag(new String(bytes, NBTConstants.CHARSET));
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                if (childType == NBTConstants.TYPE_LIST) {
                    childType = NBTConstants.TYPE_COMPOUND;
                }
                length = is.readInt();
                List<Tag> tagList = new ArrayList<Tag>();
                for (int i = 0; i < length; ++i) {
                    Tag tag = readTagPayload(childType, depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    tagList.add(tag);
                }

                return new ListTag(NBTUtils.getTypeClass(childType), tagList);
            case NBTConstants.TYPE_COMPOUND:
                Map<String, Tag> tagMap = new HashMap<String, Tag>();
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
            case NBTConstants.TYPE_INT_ARRAY: {
                length = is.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readInt();
                }
                return new IntArrayTag(data);
            }
            case NBTConstants.TYPE_LONG_ARRAY: {
                length = is.readInt();
                long[] data = new long[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readLong();
                }
                return new LongArrayTag(data);
            }
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    @Override
    public void close() throws IOException {
        if (is instanceof AutoCloseable) {
            try {
                ((AutoCloseable) is).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
