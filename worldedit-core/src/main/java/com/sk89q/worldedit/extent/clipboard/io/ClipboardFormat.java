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

package com.sk89q.worldedit.extent.clipboard.io;

import com.fastasyncworldedit.core.extent.clipboard.URIClipboardHolder;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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
    default boolean isFormat(File file) {
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            return isFormat(stream);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Return whether the given stream is of this format.
     *
     * @apiNote The caller is responsible for the following:
     *     <ul>
     *         <li>Closing the input stream</li>
     *     </ul>
     *
     * @param inputStream The stream
     * @return true if the given stream is of this format
     * @since TODO
     */
    default boolean isFormat(InputStream inputStream) {
        return false;
    }

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

    //FAWE start

    /**
     * Get the explicit file extensions (e.g. .schem2) this format is commonly known to use.
     *
     * @return The explicit file extensions this format might be known by
     */
    Set<String> getExplicitFileExtensions();

    /**
     * Sets the actor's clipboard.
     *
     * @param actor       the actor
     * @param uri         the URI of the schematic to hold
     * @param inputStream the input stream
     * @throws IOException thrown on I/O error
     */
    default URIClipboardHolder hold(Actor actor, URI uri, InputStream inputStream) throws IOException {
        checkNotNull(actor);
        checkNotNull(uri);
        checkNotNull(inputStream);

        final ClipboardReader reader = getReader(inputStream);

        final Clipboard clipboard;

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        session.setClipboard(null);
        clipboard = reader.read(actor.getUniqueId());
        URIClipboardHolder holder = new URIClipboardHolder(uri, clipboard);
        session.setClipboard(holder);
        return holder;
    }

    default Clipboard load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    default Clipboard load(InputStream stream) throws IOException {
        return getReader(stream).read();
    }

    default URL upload(final Clipboard clipboard) {
        return MainUtil.upload(null, null, getPrimaryFileExtension(), new RunnableVal<>() {
            @Override
            public void run(OutputStream value) {
                write(value, clipboard);
            }
        });
    }

    default void write(OutputStream value, Clipboard clipboard) {
        try {
            try (ParallelGZIPOutputStream gzip = new ParallelGZIPOutputStream(value)) {
                try (ClipboardWriter writer = getWriter(gzip)) {
                    writer.write(clipboard);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //FAWE end
}
