package com.fastasyncworldedit.forge1710.internal;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import org.enginehub.linbus.tree.LinByteArrayTag;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinEndTag;
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

/** Converts between Forge 1.7.10 and lin-bus NBT tags. */
public final class NBTConverter {

    private NBTConverter() {
    }

    public static LinCompoundTag toLin(NBTTagCompound tag) {
        return (LinCompoundTag) toLin((NBTBase) tag);
    }

    public static NBTTagCompound toNative(LinCompoundTag tag) {
        return (NBTTagCompound) toNative((LinTag<?>) tag);
    }

    public static FaweCompoundTag toFawe(NBTTagCompound tag) {
        return FaweCompoundTag.of(toLin(tag));
    }

    public static NBTTagCompound toNative(FaweCompoundTag tag) {
        return toNative(tag.linTag());
    }

    public static LinTag<?> toLin(NBTBase tag) {
        if (tag == null) {
            return null;
        }
        return switch (tag.getId()) {
            case 0 -> LinEndTag.instance();
            case 1 -> LinByteTag.of(((NBTTagByte) tag).func_150290_f());
            case 2 -> LinShortTag.of(((NBTTagShort) tag).func_150289_e());
            case 3 -> LinIntTag.of(((NBTTagInt) tag).func_150287_d());
            case 4 -> LinLongTag.of(((NBTTagLong) tag).func_150291_c());
            case 5 -> LinFloatTag.of(((NBTTagFloat) tag).func_150288_h());
            case 6 -> LinDoubleTag.of(((NBTTagDouble) tag).func_150286_g());
            case 7 -> LinByteArrayTag.of(((NBTTagByteArray) tag).func_150292_c());
            case 8 -> LinStringTag.of(stringValue((NBTTagString) tag));
            case 9 -> toLinList((NBTTagList) tag);
            case 10 -> toLinCompound((NBTTagCompound) tag);
            case 11 -> LinIntArrayTag.of(((NBTTagIntArray) tag).func_150302_c());
            default -> throw new IllegalArgumentException("Unknown native NBT tag id: " + tag.getId());
        };
    }

    public static NBTBase toNative(LinTag<?> tag) {
        if (tag == null) {
            return null;
        }
        if (tag instanceof LinEndTag) {
            return new NBTTagEnd();
        } else if (tag instanceof LinByteTag byteTag) {
            return new NBTTagByte(byteTag.valueAsByte());
        } else if (tag instanceof LinShortTag shortTag) {
            return new NBTTagShort(shortTag.valueAsShort());
        } else if (tag instanceof LinIntTag intTag) {
            return new NBTTagInt(intTag.valueAsInt());
        } else if (tag instanceof LinLongTag longTag) {
            return new NBTTagLong(longTag.valueAsLong());
        } else if (tag instanceof LinFloatTag floatTag) {
            return new NBTTagFloat(floatTag.valueAsFloat());
        } else if (tag instanceof LinDoubleTag doubleTag) {
            return new NBTTagDouble(doubleTag.valueAsDouble());
        } else if (tag instanceof LinByteArrayTag byteArrayTag) {
            return new NBTTagByteArray(byteArrayTag.value());
        } else if (tag instanceof LinStringTag stringTag) {
            return new NBTTagString(stringTag.value());
        } else if (tag instanceof LinListTag<?> listTag) {
            NBTTagList nativeList = new NBTTagList();
            for (LinTag<?> element : listTag.value()) {
                nativeList.appendTag(toNative(element));
            }
            return nativeList;
        } else if (tag instanceof LinCompoundTag compoundTag) {
            return toNativeCompound(compoundTag);
        } else if (tag instanceof LinIntArrayTag intArrayTag) {
            return new NBTTagIntArray(intArrayTag.value());
        } else if (tag instanceof LinLongArrayTag longArrayTag) {
            NBTTagList nativeList = new NBTTagList();
            for (long value : longArrayTag.value()) {
                nativeList.appendTag(new NBTTagLong(value));
            }
            return nativeList;
        }
        throw new IllegalArgumentException("Unknown lin-bus NBT tag type: " + tag.getClass().getCanonicalName());
    }

    private static LinCompoundTag toLinCompound(NBTTagCompound tag) {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        for (Object key : tag.func_150296_c()) {
            String name = (String) key;
            builder.put(name, toLin(tag.getTag(name)));
        }
        return builder.build();
    }

    private static String stringValue(NBTTagString tag) {
        NBTTagList list = new NBTTagList();
        list.appendTag(tag);
        return list.getStringTagAt(0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LinListTag<?> toLinList(NBTTagList tag) {
        LinTagType elementType = tag.tagCount() == 0
                ? LinTagType.endTag()
                : linTypeForNativeId((byte) tag.func_150303_d());
        List<LinTag<?>> values = new ArrayList<>(tag.tagCount());
        NBTTagList copy = (NBTTagList) tag.copy();
        while (copy.tagCount() > 0) {
            values.add(toLin(copy.removeTag(0)));
        }
        return LinListTag.of(elementType, values);
    }

    private static NBTTagCompound toNativeCompound(LinCompoundTag tag) {
        NBTTagCompound nativeTag = new NBTTagCompound();
        for (var entry : tag.value().entrySet()) {
            nativeTag.setTag(entry.getKey(), toNative(entry.getValue()));
        }
        return nativeTag;
    }

    private static LinTagType<? extends LinTag<?>> linTypeForNativeId(byte id) {
        return switch (id) {
            case 0 -> LinTagType.endTag();
            case 1 -> LinTagType.byteTag();
            case 2 -> LinTagType.shortTag();
            case 3 -> LinTagType.intTag();
            case 4 -> LinTagType.longTag();
            case 5 -> LinTagType.floatTag();
            case 6 -> LinTagType.doubleTag();
            case 7 -> LinTagType.byteArrayTag();
            case 8 -> LinTagType.stringTag();
            case 9 -> LinTagType.listTag();
            case 10 -> LinTagType.compoundTag();
            case 11 -> LinTagType.intArrayTag();
            default -> throw new IllegalArgumentException("Unknown native NBT tag id: " + id);
        };
    }
}
