package com.fastasyncworldedit.core.extent.clipboard.io;

import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.world.DataFixer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("removal") // JNBT
public class FastSchematicReaderV3 implements ClipboardReader {

    private final DataInputStream dataStream;
    private final NBTInputStream nbtStream;

    public FastSchematicReaderV3(DataInputStream dataInputStream, NBTInputStream inputStream) {
        this.dataStream = Objects.requireNonNull(dataInputStream, "dataInputStream");
        this.nbtStream = Objects.requireNonNull(inputStream, "inputStream");
    }

    @Override
    public Clipboard read() throws IOException {
        final DataFixer dataFixer =
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataFixer();
        dataStream.skipNBytes(1 + 2); // 1 Byte = TAG_Compound, 2 Bytes = Short (Length of tag name = "")
        dataStream.skipNBytes(1 + 2 + 9); // as above + 9 bytes = "Schematic"

        byte type;
        while ((type = dataStream.readByte()) != NBTConstants.TYPE_END) {
            
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        nbtStream.close(); // closes the DataInputStream implicitly
    }

}
