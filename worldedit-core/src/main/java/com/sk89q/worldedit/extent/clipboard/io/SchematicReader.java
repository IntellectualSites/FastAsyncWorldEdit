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

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

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


    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
