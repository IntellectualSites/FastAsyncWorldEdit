package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.schematic.StructureFormat;
import com.google.common.io.ByteSource;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.SchematicReader;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

public class LazyClipboardHolder extends URIClipboardHolder {
    private final ByteSource source;
    private final ClipboardFormat format;
    private final UUID uuid;
    private Clipboard clipboard;

    /**
     * Create a new instance with the given clipboard.
     *
     */
    public LazyClipboardHolder(URI uri, ByteSource source, ClipboardFormat format, UUID uuid) {
        super(uri, EmptyClipboard.INSTANCE);
        this.source = source;
        this.format = format;
        this.uuid = uuid != null ? uuid : UUID.randomUUID();
    }

    @Override
    public boolean contains(Clipboard clipboard) {
        return this.clipboard == clipboard;
    }

    @Override
    public synchronized Clipboard getClipboard() {
        if (clipboard == null) {
            try {
                try (InputStream in = source.openBufferedStream()) {
                    final ClipboardReader reader = format.getReader(in);
                    final Clipboard clipboard;
                    this.clipboard = reader.read(uuid);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return clipboard;
    }

    @Override
    public URI getURI(Clipboard clipboard) {
        return (this.clipboard == clipboard) ? getUri() : null;
    }

    @Override
    public synchronized void close() {
        if (clipboard instanceof BlockArrayClipboard) {
            ((BlockArrayClipboard) clipboard).close();
        }
        clipboard = null;
    }
}
