package com.fastasyncworldedit.core.jnbt;

import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicWriterV2;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.internal.io.FastByteArraysInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.IOException;

public class CompressedSchematicTag extends CompressedCompoundTag<Clipboard> {

    public CompressedSchematicTag(Clipboard holder) {
        super(holder);
    }

    @Override
    public LZ4BlockInputStream adapt(Clipboard src) throws IOException {
        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4out = new LZ4BlockOutputStream(blocksOut)) {
            NBTOutputStream nbtOut = new NBTOutputStream(lz4out);
            new FastSchematicWriterV2(nbtOut).write(getSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FastByteArraysInputStream in = new FastByteArraysInputStream(blocksOut.toByteArrays());
        return new LZ4BlockInputStream(in);
    }

}
