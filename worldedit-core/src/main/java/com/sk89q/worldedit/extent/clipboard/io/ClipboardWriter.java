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

import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes {@code Clipboard}s.
 *
 * @see Clipboard
 */
public interface ClipboardWriter extends Closeable {

    /**
     * Writes a clipboard.
     *
     * @param clipboard the clipboard
     * @throws IOException thrown on I/O error
     */
    void write(Clipboard clipboard) throws IOException;

    default Tag writeVector(Vector3 vector) {
        List<DoubleTag> list = new ArrayList<>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    default Tag writeRotation(Location location) {
        List<FloatTag> list = new ArrayList<>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }
}
