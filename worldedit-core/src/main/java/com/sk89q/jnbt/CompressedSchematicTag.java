package com.sk89q.jnbt;

import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicWriter;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class CompressedSchematicTag extends CompressedCompoundTag<Clipboard> {
    public CompressedSchematicTag(Clipboard holder) {
        super(holder);
    }

    @Override
    public DataInputStream adapt(Clipboard src) throws IOException {
        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4out = new LZ4BlockOutputStream(blocksOut)) {
            NBTOutputStream nbtOut = new NBTOutputStream(lz4out);
            new SpongeSchematicWriter(nbtOut).write(getSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FastByteArraysInputStream in = new FastByteArraysInputStream(blocksOut.toByteArrays());
        return new DataInputStream(new LZ4BlockInputStream(in));
    }
}
