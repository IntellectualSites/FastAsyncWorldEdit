package com.fastasyncworldedit.nukkit;

import cn.nukkit.nbt.tag.ByteArrayTag;
import cn.nukkit.nbt.tag.ByteTag;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.IntArrayTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.LongTag;
import cn.nukkit.nbt.tag.ShortTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.nbt.tag.Tag;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import org.enginehub.linbus.common.LinTagId;
import org.enginehub.linbus.tree.LinByteArrayTag;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinLongArrayTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Converts between Nukkit NBT tags and FAWE LinBus tags.
 */
public final class NukkitNbtConverter {

    private NukkitNbtConverter() {
    }

    /**
     * Convert a Nukkit CompoundTag to a FaweCompoundTag.
     */
    public static FaweCompoundTag toFawe(CompoundTag nukkitTag) {
        return FaweCompoundTag.of(toLinCompound(nukkitTag));
    }

    /**
     * Convert a FaweCompoundTag to a Nukkit CompoundTag.
     */
    public static CompoundTag toNukkit(FaweCompoundTag faweTag) {
        return toNukkitCompound(faweTag.linTag());
    }

    /**
     * Convert a Nukkit CompoundTag to a LinCompoundTag.
     */
    public static LinCompoundTag toLinCompound(CompoundTag nukkitTag) {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        for (Map.Entry<String, Tag> entry : nukkitTag.getTags().entrySet()) {
            LinTag<?> linTag = toLinTag(entry.getValue());
            if (linTag != null) {
                builder.put(entry.getKey(), linTag);
            }
        }
        return builder.build();
    }

    /**
     * Convert a LinCompoundTag to a Nukkit CompoundTag.
     */
    public static CompoundTag toNukkitCompound(LinCompoundTag linTag) {
        CompoundTag result = new CompoundTag();
        for (Map.Entry<String, LinTag<?>> entry : linTag.value().entrySet()) {
            Tag nukkitTag = toNukkitTag(entry.getValue());
            if (nukkitTag != null) {
                result.put(entry.getKey(), nukkitTag);
            }
        }
        return result;
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static LinTag<?> toLinTag(Tag nukkitTag) {
        return switch (nukkitTag.getId()) {
            case Tag.TAG_Byte -> LinByteTag.of((byte) ((ByteTag) nukkitTag).data);
            case Tag.TAG_Short -> LinShortTag.of((short) ((ShortTag) nukkitTag).data);
            case Tag.TAG_Int -> LinIntTag.of(((IntTag) nukkitTag).data);
            case Tag.TAG_Long -> LinLongTag.of(((LongTag) nukkitTag).data);
            case Tag.TAG_Float -> LinFloatTag.of(((FloatTag) nukkitTag).data);
            case Tag.TAG_Double -> LinDoubleTag.of(((DoubleTag) nukkitTag).data);
            case Tag.TAG_Byte_Array -> LinByteArrayTag.of(((ByteArrayTag) nukkitTag).getData());
            case Tag.TAG_String -> LinStringTag.of(((StringTag) nukkitTag).data);
            case Tag.TAG_List -> {
                ListTag<? extends Tag> listTag = (ListTag<? extends Tag>) nukkitTag;
                if (listTag.size() == 0) {
                    yield LinListTag.empty(LinTagType.endTag());
                }
                byte elementType = listTag.type;
                LinTagId linId = LinTagId.fromId(elementType);
                LinListTag.Builder builder = LinListTag.builder(LinTagType.fromId(linId));
                for (Tag item : listTag.getAll()) {
                    LinTag<?> converted = toLinTag(item);
                    if (converted != null) {
                        builder.add(converted);
                    }
                }
                yield builder.build();
            }
            case Tag.TAG_Compound -> toLinCompound((CompoundTag) nukkitTag);
            case Tag.TAG_Int_Array -> LinIntArrayTag.of(((IntArrayTag) nukkitTag).getData());
            default -> null;
        };
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Tag toNukkitTag(LinTag<?> linTag) {
        return switch (linTag.type().id()) {
            case BYTE -> new ByteTag("", ((LinByteTag) linTag).valueAsByte());
            case SHORT -> new ShortTag("", ((LinShortTag) linTag).valueAsShort());
            case INT -> new IntTag("", ((LinIntTag) linTag).valueAsInt());
            case LONG -> new LongTag("", ((LinLongTag) linTag).valueAsLong());
            case FLOAT -> new FloatTag("", ((LinFloatTag) linTag).valueAsFloat());
            case DOUBLE -> new DoubleTag("", ((LinDoubleTag) linTag).valueAsDouble());
            case BYTE_ARRAY -> new ByteArrayTag("", ((LinByteArrayTag) linTag).value());
            case STRING -> new StringTag("", ((LinStringTag) linTag).value());
            case LIST -> {
                LinListTag<?> linList = (LinListTag<?>) linTag;
                ListTag nukkitList = new ListTag<>();
                for (LinTag<?> item : linList.value()) {
                    Tag converted = toNukkitTag(item);
                    if (converted != null) {
                        nukkitList.add(converted);
                    }
                }
                yield nukkitList;
            }
            case COMPOUND -> toNukkitCompound((LinCompoundTag) linTag);
            case INT_ARRAY -> new IntArrayTag("", ((LinIntArrayTag) linTag).value());
            case LONG_ARRAY -> {
                // Nukkit doesn't have LongArrayTag; store as compound with metadata
                long[] values = ((LinLongArrayTag) linTag).value();
                // Best effort: convert to int array if values fit, otherwise skip
                int[] intValues = new int[values.length * 2];
                for (int i = 0; i < values.length; i++) {
                    intValues[i * 2] = (int) (values[i] >> 32);
                    intValues[i * 2 + 1] = (int) values[i];
                }
                yield new IntArrayTag("", intValues);
            }
            default -> null;
        };
    }

}
