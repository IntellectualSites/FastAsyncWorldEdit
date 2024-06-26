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

import com.fastasyncworldedit.core.internal.io.LittleEndianOutputStream;
import org.enginehub.linbus.stream.LinBinaryIO;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

// THIS CLASS HAS BEEN HEAVILY MODIFIED BY FAWE

/**
 * This class writes <strong>NBT</strong>, or <strong>Named Binary Tag</strong>
 * {@code Tag} objects to an underlying {@code OutputStream}.
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
public final class NBTOutputStream extends OutputStream implements Closeable, DataOutput {

    /**
     * The output stream.
     */
    private final DataOutput os;

    /**
     * Creates a new {@code NBTOutputStream}, which will write data to the
     * specified underlying output stream.
     *
     * @param os The output stream.
     * @throws IOException if an I/O error occurs.
     */
    public NBTOutputStream(OutputStream os) {
        this(os instanceof DataOutput ? (DataOutput) os : new DataOutputStream(os));
    }

    // Don't delete
    public NBTOutputStream(DataOutput os) {
        this.os = os;
    }

    public NBTOutputStream(DataOutputStream os) {
        this.os = os;
    }

    // Don't delete
    public NBTOutputStream(OutputStream os, boolean littleEndian) throws IOException {
        this(littleEndian ? new LittleEndianOutputStream(os) : os);
    }

    public DataOutput getOutputStream() {
        return os;
    }

    //TODO writeNamedTag was changed in upstream so the code below this comment may not work anymore.

    /**
     * Writes a tag.
     *
     * @param tag The tag to write.
     * @throws IOException if an I/O error occurs.
     */
    public void writeNamedTag(String name, Tag<?, ?> tag) throws IOException {
        checkNotNull(name);
        checkNotNull(tag);

        int type = tag.getTypeCode();
        writeNamedTagName(name, type);

        if (type == NBTConstants.TYPE_END) {
            throw new IOException("Named TAG_End not permitted.");
        }

        writeTagPayload(tag);
    }

    public void writeNamedTag(String name, String value) throws IOException {
        checkNotNull(name);
        checkNotNull(value);
        int type = NBTConstants.TYPE_STRING;
        writeNamedTagName(name, type);
        byte[] bytes = value.getBytes(NBTConstants.CHARSET);
        os.writeShort(bytes.length);
        os.write(bytes);
    }

    public void writeNamedTag(String name, int value) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_INT;
        writeNamedTagName(name, type);
        os.writeInt(value);
    }

    public void writeNamedTag(String name, byte value) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_BYTE;
        writeNamedTagName(name, type);
        os.writeByte(value);
    }

    public void writeNamedTag(String name, short value) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_SHORT;
        writeNamedTagName(name, type);
        os.writeShort(value);
    }

    public void writeNamedTag(String name, long value) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_LONG;
        writeNamedTagName(name, type);
        os.writeLong(value);
    }

    public void writeNamedTag(String name, byte[] bytes) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_BYTE_ARRAY;
        writeNamedTagName(name, type);
        os.writeInt(bytes.length);
        os.write(bytes);
    }

    public void writeNamedTag(String name, int[] data) throws IOException {
        checkNotNull(name);
        int type = NBTConstants.TYPE_INT_ARRAY;
        writeNamedTagName(name, type);
        os.writeInt(data.length);
        for (int aData : data) {
            os.writeInt(aData);
        }
    }

    public void writeNamedEmptyList(String name) throws IOException {
        writeNamedEmptyList(name, NBTConstants.TYPE_COMPOUND);
    }

    private void writeNamedEmptyList(String name, int type) throws IOException {
        writeNamedTagName(name, NBTConstants.TYPE_LIST);
        os.writeByte(type);
        os.writeInt(0);
    }

    public void writeNamedTagName(String name, int type) throws IOException {
        //        byte[] nameBytes = name.getBytes(NBTConstants.CHARSET);

        os.writeByte(type);
        os.writeUTF(name);
        //        os.writeShort(nameBytes.length);
        //        os.write(nameBytes);
    }

    public void writeLazyCompoundTag(String name, LazyWrite next) throws IOException {
        byte[] nameBytes = name.getBytes(NBTConstants.CHARSET);
        os.writeByte(NBTConstants.TYPE_COMPOUND);
        os.writeShort(nameBytes.length);
        os.write(nameBytes);
        next.write(this);
        os.writeByte(NBTConstants.TYPE_END);
    }

    public interface LazyWrite {

        void write(NBTOutputStream out) throws IOException;

    }

    public void writeTag(Tag tag) throws IOException {
        int type = tag.getTypeCode();
        os.writeByte(type);
        writeTagPayload(tag);
    }

    public void writeEndTag() throws IOException {
        os.writeByte(NBTConstants.TYPE_END);
    }

    /**
     * Writes tag payload.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    public void writeTagPayload(Tag tag) throws IOException {
        int type = tag.getTypeCode();
        switch (type) {
            case NBTConstants.TYPE_END:
                writeEndTagPayload((EndTag) tag);
                break;
            case NBTConstants.TYPE_BYTE:
                writeByteTagPayload((ByteTag) tag);
                break;
            case NBTConstants.TYPE_SHORT:
                writeShortTagPayload((ShortTag) tag);
                break;
            case NBTConstants.TYPE_INT:
                writeIntTagPayload((IntTag) tag);
                break;
            case NBTConstants.TYPE_LONG:
                writeLongTagPayload((LongTag) tag);
                break;
            case NBTConstants.TYPE_FLOAT:
                writeFloatTagPayload((FloatTag) tag);
                break;
            case NBTConstants.TYPE_DOUBLE:
                writeDoubleTagPayload((DoubleTag) tag);
                break;
            case NBTConstants.TYPE_BYTE_ARRAY:
                writeByteArrayTagPayload((ByteArrayTag) tag);
                break;
            case NBTConstants.TYPE_STRING:
                writeStringTagPayload((StringTag) tag);
                break;
            case NBTConstants.TYPE_LIST:
                writeListTagPayload((ListTag) tag);
                break;
            case NBTConstants.TYPE_COMPOUND:
                writeCompoundTagPayload((CompoundTag) tag);
                break;
            case NBTConstants.TYPE_INT_ARRAY:
                writeIntArrayTagPayload((IntArrayTag) tag);
                break;
            case NBTConstants.TYPE_LONG_ARRAY:
                writeLongArrayTagPayload((LongArrayTag) tag);
                break;
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    /**
     * Writes a {@code TAG_Byte} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeByteTagPayload(ByteTag tag) throws IOException {
        os.writeByte(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Byte_Array} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeByteArrayTagPayload(ByteArrayTag tag) throws IOException {
        byte[] bytes = tag.getValue();
        os.writeInt(bytes.length);
        os.write(bytes);
    }

    /**
     * Writes a {@code TAG_Compound} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeCompoundTagPayload(CompoundTag tag) throws IOException {
        for (Map.Entry<String, Tag<?, ?>> entry : tag.getValue().entrySet()) {
            writeNamedTag(entry.getKey(), entry.getValue());
        }
        os.writeByte((byte) 0); // end tag - better way?
    }

    /**
     * Writes a {@code TAG_List} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeListTagPayload(ListTag tag) throws IOException {
        Class<? extends Tag<?, ?>> clazz = tag.getType();
        if (clazz == null) {
            clazz = CompoundTag.class;
        }
        List<Tag> tags = tag.getValue();
        int size = tags.size();
        if (!tags.isEmpty()) {
            Tag tag0 = tags.get(0);
            os.writeByte(tag0.getTypeCode());
        } else {
            os.writeByte(NBTUtils.getTypeCode(clazz));
        }
        os.writeInt(size);
        for (Tag tag1 : tags) {
            writeTagPayload(tag1);
        }
    }

    /**
     * Writes a {@code TAG_String} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeStringTagPayload(StringTag tag) throws IOException {
        byte[] bytes = tag.getValue().getBytes(NBTConstants.CHARSET);
        os.writeShort(bytes.length);
        os.write(bytes);
    }

    /**
     * Writes a {@code TAG_Double} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeDoubleTagPayload(DoubleTag tag) throws IOException {
        os.writeDouble(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Float} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeFloatTagPayload(FloatTag tag) throws IOException {
        os.writeFloat(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Long} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeLongTagPayload(LongTag tag) throws IOException {
        os.writeLong(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Int} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeIntTagPayload(IntTag tag) throws IOException {
        os.writeInt(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Short} tag.
     *
     * @param tag The tag.
     * @throws IOException if an I/O error occurs.
     */
    private void writeShortTagPayload(ShortTag tag) throws IOException {
        os.writeShort(tag.getValue());
    }

    /**
     * Writes a {@code TAG_Empty} tag.
     *
     * @param tag the tag
     */
    private void writeEndTagPayload(EndTag tag) {
        /* empty */
    }

    private void writeIntArrayTagPayload(IntArrayTag tag) throws IOException {
        int[] data = tag.getValue();
        os.writeInt(data.length);
        for (int aData : data) {
            os.writeInt(aData);
        }
    }

    private void writeLongArrayTagPayload(LongArrayTag tag) throws IOException {
        long[] data = tag.getValue();
        os.writeInt(data.length);
        for (long aData : data) {
            os.writeLong(aData);
        }
    }

    @Override
    public void close() throws IOException {
        if (os instanceof Closeable) {
            ((Closeable) os).close();
        }
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        os.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        os.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        os.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        os.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        os.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        os.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        os.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        os.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        os.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        os.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        os.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        os.writeUTF(s);
    }

    /**
     * Flush output.
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        if (os instanceof Flushable) {
            ((Flushable) os).flush();
        }
    }

}
