package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.BinaryTagType;
import com.sk89q.worldedit.util.nbt.BinaryTagTypes;
import com.sk89q.worldedit.util.nbt.ByteBinaryTag;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.IntBinaryTag;
import com.sk89q.worldedit.util.nbt.ShortBinaryTag;
import com.sk89q.worldedit.world.storage.InvalidFormatException;

import java.util.HashMap;
import java.util.Map;

public class NbtUtils {

    /**
     * Get child tag of a NBT structure.
     *
     * @param tag      the tag to read from
     * @param key      the key to look for
     * @param expected the expected NBT class type
     * @return child tag
     * @throws InvalidFormatException if the format of the items is invalid
     */
    public static <T extends BinaryTag> T getChildTag(CompoundBinaryTag tag, String key, BinaryTagType<T> expected) throws
            InvalidFormatException {
        BinaryTag childTag = tag.get(key);
        if (childTag == null) {
            throw new InvalidFormatException("Missing a \"" + key + "\" tag");
        }

        if (childTag.type().id() != expected.id()) {
            throw new InvalidFormatException(key + " tag is not of tag type " + expected);
        }
        // SAFETY: same binary tag type checked above
        @SuppressWarnings("unchecked")
        T childTagCast = (T) childTag;
        return childTagCast;
    }

    /**
     * Get an integer from a tag.
     *
     * @param tag the tag to read from
     * @param key the key to look for
     * @return child tag
     * @throws InvalidFormatException if the format of the items is invalid
     */
    public static int getInt(CompoundBinaryTag tag, String key) throws InvalidFormatException {
        BinaryTag childTag = tag.get(key);
        if (childTag == null) {
            throw new InvalidFormatException("Missing a \"" + key + "\" tag");
        }

        BinaryTagType<?> type = childTag.type();
        if (type == BinaryTagTypes.INT) {
            return ((IntBinaryTag) childTag).intValue();
        }
        if (type == BinaryTagTypes.BYTE) {
            return ((ByteBinaryTag) childTag).intValue();
        }
        if (type == BinaryTagTypes.SHORT) {
            return ((ShortBinaryTag) childTag).intValue();
        }
        throw new InvalidFormatException(key + " tag is not of int, short or byte tag type.");
    }

    /**
     * Get a mutable map of the values stored inside a {@link CompoundBinaryTag}
     *
     * @param tag {@link CompoundBinaryTag} to get values for
     * @return Mutable map of values
     */
    public static Map<String, BinaryTag> getCompoundBinaryTagValues(CompoundBinaryTag tag) {
        Map<String, BinaryTag> value = new HashMap<>();
        tag.forEach((e) -> value.put(e.getKey(), e.getValue()));
        return value;
    }

}
