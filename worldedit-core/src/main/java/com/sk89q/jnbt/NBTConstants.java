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

package com.sk89q.jnbt;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A class which holds constant values.
 *
 * @deprecated JNBT is being removed for adventure-nbt in WorldEdit 8.
 */
@Deprecated(forRemoval = true)
public final class NBTConstants {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int TYPE_END = 0;
    public static final int TYPE_BYTE = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_INT = 3;
    public static final int TYPE_LONG = 4;
    public static final int TYPE_FLOAT = 5;
    public static final int TYPE_DOUBLE = 6;
    public static final int TYPE_BYTE_ARRAY = 7;
    public static final int TYPE_STRING = 8;
    public static final int TYPE_LIST = 9;
    public static final int TYPE_COMPOUND = 10;
    public static final int TYPE_INT_ARRAY = 11;
    public static final int TYPE_LONG_ARRAY = 12;

    /**
     * Default private constructor.
     */
    private NBTConstants() {
    }

    /**
     * Convert a type ID to its corresponding {@link Tag} class.
     *
     * @param id type ID
     * @return tag class
     * @throws IllegalArgumentException thrown if the tag ID is not valid
     */
    public static Class<? extends Tag<?, ?>> getClassFromType(int id) {
        return switch (id) {
            case TYPE_END -> EndTag.class;
            case TYPE_BYTE -> ByteTag.class;
            case TYPE_SHORT -> ShortTag.class;
            case TYPE_INT -> IntTag.class;
            case TYPE_LONG -> LongTag.class;
            case TYPE_FLOAT -> FloatTag.class;
            case TYPE_DOUBLE -> DoubleTag.class;
            case TYPE_BYTE_ARRAY -> ByteArrayTag.class;
            case TYPE_STRING -> StringTag.class;
            case TYPE_LIST -> {
                @SuppressWarnings("unchecked")
                var aClass = (Class<? extends Tag<?, ?>>) (Class<?>) ListTag.class;
                yield aClass;
            }
            case TYPE_COMPOUND -> CompoundTag.class;
            case TYPE_INT_ARRAY -> IntArrayTag.class;
            case TYPE_LONG_ARRAY -> LongArrayTag.class;
            default -> throw new IllegalArgumentException("Unknown tag type ID of " + id);
        };
    }

}
