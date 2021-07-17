package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.BinaryTagType;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.storage.InvalidFormatException;

public class NbtUtils {

    /**
     * Get child tag of a NBT structure.
     *
     * @param tag the tag to read from
     * @param key the key to look for
     * @param expected the expected NBT class type
     * @return child tag
     * @throws InvalidFormatException if the format of the items is invalid
     */
    public static <T extends BinaryTag> T getChildTag(CompoundBinaryTag tag, String key, BinaryTagType<T> expected) throws InvalidFormatException {
        BinaryTag childTag = tag.get(key);
        if (childTag == null) {
            throw new InvalidFormatException("Missing a \"" + key + "\" tag");
        }

        if (childTag.type().id() != expected.id()) {
            throw new InvalidFormatException(key + " tag is not of tag type " + expected.toString());
        }
        // SAFETY: same binary tag type checked above
        @SuppressWarnings("unchecked")
        T childTagCast = (T) childTag;
        return childTagCast;
    }

}
