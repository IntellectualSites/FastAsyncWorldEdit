package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public interface IClipboardFormat {
    /**
     * Returns the name of this format.
     *
     * @return The name of the format
     */
    String getName();

    /**
     * Create a reader.
     *
     * @param inputStream the input stream
     * @return a reader
     * @throws java.io.IOException thrown on I/O error
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
     * Get the default extension
     *
     * @return
     */
    String getExtension();

    /**
     * Get a set of aliases.
     *
     * @return a set of aliases
     */
    Set<String> getAliases();
}