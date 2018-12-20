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

/**
 * A collection of supported clipboard formats.
 */
@Deprecated
public class BuiltInClipboardFormat {
    public static final ClipboardFormat MCEDIT_SCHEMATIC = ClipboardFormat.SCHEMATIC;
    public static final ClipboardFormat SPONGE_SCHEMATIC = ClipboardFormat.SPONGE_SCHEMATIC;
    public static final ClipboardFormat STRUCTURE = ClipboardFormat.STRUCTURE;
    public static final ClipboardFormat PNG = ClipboardFormat.PNG;

    @Deprecated
    public static final ClipboardFormat[] values() {
        return ClipboardFormat.values;
    }

    @Deprecated
    public static ClipboardFormat valueOf(String value) {
        switch (value) {
            case "MCEDIT_SCHEMATIC":
                value = "SCHEMATIC";
                break;
        }
        return ClipboardFormat.valueOf(value);
    }
}