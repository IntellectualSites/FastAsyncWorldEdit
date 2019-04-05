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

package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.CorruptSchematicStreamer;
import com.boydti.fawe.jnbt.SchematicStreamer;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicReader implements ClipboardReader {

    private NBTInputStream inputStream;
    private InputStream rootStream;

    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public SchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
    }

    public void setUnderlyingStream(InputStream in) {
        this.rootStream = in;
    }

    @Override
    public Clipboard read() throws IOException {
        return read(UUID.randomUUID());
    }

    public Clipboard read(final UUID clipboardId) throws IOException {
        try {
            return new SchematicStreamer(inputStream, clipboardId).getClipboard();
        } catch (Exception e) {
            Fawe.debug("Input is corrupt!");
            e.printStackTrace();
            return new CorruptSchematicStreamer(rootStream, clipboardId).recover();
        }
    }

    private static <T extends Tag> T requireTag(Map<String, Tag> items, String key, Class<T> expected) throws IOException {
        if (!items.containsKey(key)) {
            throw new IOException("Schematic file is missing a \"" + key + "\" tag");
        }

        Tag tag = items.get(key);
        if (!expected.isInstance(tag)) {
            throw new IOException(key + " tag is not of tag type " + expected.getName());
        }

        return expected.cast(tag);
    }

    @Nullable
    private static <T extends Tag> T getTag(CompoundTag tag, Class<T> expected, String key) {
        Map<String, Tag> items = tag.getValue();

        if (!items.containsKey(key)) {
            return null;
        }

        Tag test = items.get(key);
        if (!expected.isInstance(test)) {
            return null;
        }

        return expected.cast(test);
    }



    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
