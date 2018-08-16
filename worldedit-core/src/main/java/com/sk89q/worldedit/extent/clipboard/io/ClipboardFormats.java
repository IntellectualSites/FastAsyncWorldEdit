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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClipboardFormats {
    /**
     * Find the clipboard format named by the given alias.
     *
     * @param alias
     *            the alias
     * @return the format, otherwise null if none is matched
     */
    @Nullable
    public static ClipboardFormat findByAlias(String alias) {
        return ClipboardFormat.findByAlias(alias);
    }

    /**
     * Detect the format of given a file.
     *
     * @param file
     *            the file
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByFile(File file) {
        checkNotNull(file);

        for (ClipboardFormat format : ClipboardFormat.values()) {
            if (format.isFormat(file)) {
                return format;
            }
        }

        return null;
    }

    /**
     * @return a multimap from a file extension to the potential matching formats.
     */
    public static Multimap<String, ClipboardFormat> getFileExtensionMap() {
        HashMultimap<String, ClipboardFormat> map = HashMultimap.create();
        for (ClipboardFormat format : ClipboardFormat.values()) {
            for (String ext : format.getFileExtensions()) {
                map.put(ext, format);
            }
        }
        return map;
    }

    public static Collection<ClipboardFormat> getAll() {
        return Arrays.asList(ClipboardFormat.values());
    }

    /**
     * Not public API, only used by SchematicCommands.
     * It is not in SchematicCommands because it may rely on internal register calls.
     */
    public static String[] getFileExtensionArray() {
        List<String> exts = new ArrayList<>();
        HashMultimap<String, ClipboardFormat> map = HashMultimap.create();
        for (ClipboardFormat format : ClipboardFormat.values()) {
            exts.addAll(format.getFileExtensions());
        }
        return exts.toArray(new String[exts.size()]);
    }

    private ClipboardFormats() {}
}