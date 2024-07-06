package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.world.storage.InvalidFormatException;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

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
    public static <T extends LinTag> T getChildTag(LinCompoundTag tag, String key, LinTagType expected) throws
            InvalidFormatException {
        LinTag childTag = tag.value().get(key);
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
     * @since 2.1.0
     */
    public static int getInt(LinCompoundTag tag, String key) throws InvalidFormatException {
        LinTag childTag = tag.value().get(key);
        if (childTag == null) {
            throw new InvalidFormatException("Missing a \"" + key + "\" tag");
        }

        LinTagType<?> type = childTag.type();
        if (type == LinTagType.intTag()) {
            return ((LinIntTag) childTag).value();
        }
        if (type == LinTagType.byteTag()) {
            return ((LinByteTag) childTag).value();
        }
        if (type == LinTagType.shortTag()) {
            return ((LinShortTag) childTag).value();
        }
        throw new InvalidFormatException(key + " tag is not of int, short or byte tag type.");
    }

    /**
     * Get a mutable map of the values stored inside a {@link LinCompoundTag}
     *
     * @param tag {@link LinCompoundTag} to get values for
     * @return Mutable map of values
     * @since 2.1.0
     */
    public static Map<String, LinTag<?>> getLinCompoundTagValues(LinCompoundTag tag) {
        Map<String, LinTag<?>> value = new HashMap<>();
        value.putAll(tag.value());
        return value;
    }

}
