package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.storage.InvalidFormatException;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NbtUtils {

    private NbtUtils() {
    }

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

    /**
     * Tries to extract UUID information from a compound tag
     *
     * @param compoundTag the compound tag to extract uuid information from
     * @return the extracted UUID
     * @since 2.11.2
     */
    public static UUID uuid(FaweCompoundTag compoundTag) {
        final LinCompoundTag linTag = compoundTag.linTag();
        {
            final LinIntArrayTag uuidTag = linTag.findTag("UUID", LinTagType.intArrayTag());
            if (uuidTag != null) {
                int[] arr = uuidTag.value();
                return new UUID((long) arr[0] << 32 | (arr[1] & 0xFFFFFFFFL), (long) arr[2] << 32 | (arr[3] & 0xFFFFFFFFL));
            }
        }
        {
            final LinLongTag uuidMostTag = linTag.findTag("UUIDMost", LinTagType.longTag());
            if (uuidMostTag != null) {
                return new UUID(uuidMostTag.valueAsLong(), linTag.getTag("UUIDLeast", LinTagType.longTag()).valueAsLong());
            }
        }
        {
            final LinLongTag uuidMostTag = linTag.findTag("WorldUUIDMost", LinTagType.longTag());
            if (uuidMostTag != null) {
                return new UUID(uuidMostTag.valueAsLong(), linTag.getTag("WorldUUIDLeast", LinTagType.longTag()).valueAsLong());
            }

        }
        {
            final LinLongTag uuidMostTag = linTag.findTag("PersistentIDMSB", LinTagType.longTag());
            if (uuidMostTag != null) {
                return new UUID(uuidMostTag.valueAsLong(), linTag.getTag("PersistentIDLSB", LinTagType.longTag()).valueAsLong());
            }

        }
        throw new IllegalArgumentException("no uuid present");

    }

    /**
     * Create a copy of the tag and modify the (x, y, z) coordinates
     *
     * @param tag Tag to copy
     * @param x   New X coordinate
     * @param y   New Y coordinate
     * @param z   New Z coordinate
     * @return New tag
     * @since 2.11.2
     */
    public static @Nonnull LinCompoundTag withPosition(@Nonnull LinCompoundTag tag, int x, int y, int z) {
        return tag.toBuilder()
                .putInt("x", x)
                .putInt("y", y)
                .putInt("z", z)
                .build();
    }

    /**
     * Create a copy of the tag and modify the (x, y, z) coordinates
     *
     * @param tag Tag to copy
     * @param x   New X coordinate
     * @param y   New Y coordinate
     * @param z   New Z coordinate
     * @return New tag
     * @since 2.11.2
     */
    public static @Nonnull FaweCompoundTag withPosition(@Nonnull FaweCompoundTag tag, int x, int y, int z) {
        return FaweCompoundTag.of(withPosition(tag.linTag(), x, y, z));
    }

    /**
     * {@return a copy of the given tag with the Id and the Pos of the given entity}
     *
     * @param tag    the tag to copy
     * @param entity the entity to use the Id and the Pos from
     * @since 2.11.2
     */
    public static @Nonnull LinCompoundTag withEntityInfo(@Nonnull LinCompoundTag tag, @Nonnull Entity entity) {
        final LinCompoundTag.Builder builder = tag.toBuilder()
                .putString("Id", entity.getState().getType().id());
        LinListTag<LinDoubleTag> pos = tag.findListTag("Pos", LinTagType.doubleTag());
        if (pos != null) { // TODO why only if pos != null?
            Location loc = entity.getLocation();
            final LinListTag<LinDoubleTag> newPos = LinListTag.builder(LinTagType.doubleTag())
                    .add(LinDoubleTag.of(loc.x()))
                    .add(LinDoubleTag.of(loc.y()))
                    .add(LinDoubleTag.of(loc.z()))
                    .build();
            builder.put("Pos", newPos);
        }
        return builder.build();
    }

    /**
     * Adds a UUID to the given map
     *
     * @param map  the map to insert to
     * @param uuid the uuid to insert
     * @since 2.11.2
     */
    public static void addUUIDToMap(Map<String, LinTag<?>> map, UUID uuid) {
        int[] uuidArray = new int[4];
        uuidArray[0] = (int) (uuid.getMostSignificantBits() >> 32);
        uuidArray[1] = (int) uuid.getMostSignificantBits();
        uuidArray[2] = (int) (uuid.getLeastSignificantBits() >> 32);
        uuidArray[3] = (int) uuid.getLeastSignificantBits();
        map.put("UUID", LinIntArrayTag.of(uuidArray));

        map.put("UUIDMost", LinLongTag.of(uuid.getMostSignificantBits()));
        map.put("UUIDLeast", LinLongTag.of(uuid.getLeastSignificantBits()));

        map.put("WorldUUIDMost", LinLongTag.of(uuid.getMostSignificantBits()));
        map.put("WorldUUIDLeast", LinLongTag.of(uuid.getLeastSignificantBits()));

        map.put("PersistentIDMSB", LinLongTag.of(uuid.getMostSignificantBits()));
        map.put("PersistentIDLSB", LinLongTag.of(uuid.getLeastSignificantBits()));
    }

    /**
     * {@return the position data of the given tag}
     *
     * @param compoundTag the tag to extract position information from
     * @since 2.12.0
     */
    public static Vector3 entityPosition(FaweCompoundTag compoundTag) {
        LinListTag<LinDoubleTag> pos = compoundTag.linTag().getListTag("Pos", LinTagType.doubleTag());
        double x = pos.get(0).valueAsDouble();
        double y = pos.get(1).valueAsDouble();
        double z = pos.get(2).valueAsDouble();
        return Vector3.at(x, y, z);
    }

}
