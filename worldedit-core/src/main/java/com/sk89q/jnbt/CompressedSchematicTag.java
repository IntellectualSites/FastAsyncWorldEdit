package com.sk89q.jnbt;

import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream.FastByteArrayInputStream;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicWriter;
import java.io.IOException;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

public class CompressedSchematicTag extends CompressedCompoundTag<Clipboard> {
    public CompressedSchematicTag(Clipboard holder) {
        super(holder);
    }

    @Override
    public LZ4BlockInputStream adapt(Clipboard src) throws IOException {
        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4out = new LZ4BlockOutputStream(blocksOut)) {
            NBTOutputStream nbtOut = new NBTOutputStream(lz4out);
            new SpongeSchematicWriter(nbtOut).write(getSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FastByteArrayInputStream in = new FastByteArrayInputStream(blocksOut);
        return new LZ4BlockInputStream(in);
    }
}
