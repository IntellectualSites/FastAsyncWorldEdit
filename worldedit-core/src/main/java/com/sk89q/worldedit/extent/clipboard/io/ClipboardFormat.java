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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.MainUtil;
import com.google.gson.Gson;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

/**
 * A collection of supported clipboard formats.
 */
public interface ClipboardFormat {

    /**
     * Returns the name of this format.
     *
     * @return The name of the format
     */
    String getName();

    /**
     * Get a set of aliases.
     *
     * @return a set of aliases
     */
    Set<String> getAliases();

    /**
     * Create a reader.
     *
     * @param inputStream the input stream
     * @return a reader
     * @throws IOException thrown on I/O error
     */
    ClipboardReader getReader(InputStream inputStream) throws IOException;

    /**
     * Create a writer.
     *
     * @param outputStream the output stream
     * @return a writer
     * @throws IOException thrown on I/O error
     */
    ClipboardWriter getWriter(OutputStream outputStream) throws IOException;

    /**
     * Return whether the given file is of this format.
     *
     * @param file the file
     * @return true if the given file is of this format
     */
    boolean isFormat(File file);

    /**
     * Get the file extension this format primarily uses.
     *
     * @return The primary file extension
     */
    String getPrimaryFileExtension();

    /**
     * Get the file extensions this format is commonly known to use. This should
     * include {@link #getPrimaryFileExtension()}.
     *
     * @return The file extensions this format might be known by
     */
    Set<String> getFileExtensions();
    
    /**
     * Set the player's clipboard
     * @param player
     * @param uri
     * @param in
     * @return the held clipboard
     * @throws IOException
     */
    default ClipboardHolder hold(Player player, URI uri, InputStream in) throws IOException {
        checkNotNull(player);
        checkNotNull(uri);
        checkNotNull(in);

        final ClipboardReader reader = getReader(in);

        final Clipboard clipboard;

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        session.setClipboard(null);
        clipboard = reader.read(player.getUniqueId());
        URIClipboardHolder holder = new URIClipboardHolder(uri, clipboard);
        session.setClipboard(holder);
        return holder;
    }
    
    default Schematic load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    default Schematic load(InputStream stream) throws IOException {
        return new Schematic(getReader(stream).read());
    }
    
    
    default URL uploadPublic(final Clipboard clipboard, String category, String user) {
        // summary
        // blocks
        HashMap<String, Object> map = new HashMap<>();
        BlockVector3 dimensions = clipboard.getDimensions();
        map.put("width", dimensions.getX());
        map.put("height", dimensions.getY());
        map.put("length", dimensions.getZ());
        map.put("creator", user);
        Gson gson = new Gson();
        String json = gson.toJson(map);
        return MainUtil.upload(Settings.IMP.WEB.ASSETS, false, json, category, null, new RunnableVal<OutputStream>() {
            @Override
            public void run(OutputStream value) {
                write(value, clipboard);
            }
        });
    }
    
    default URL uploadAnonymous(final Clipboard clipboard) {
        return MainUtil.upload(null, null, getPrimaryFileExtension(), new RunnableVal<OutputStream>() {
            @Override
            public void run(OutputStream value) {
                write(value, clipboard);
            }
        });
    }
    
    default void write(OutputStream value, Clipboard clipboard) {
        try {
            try (PGZIPOutputStream gzip = new PGZIPOutputStream(value)) {
                try (ClipboardWriter writer = getWriter(gzip)) {
                    writer.write(clipboard);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
