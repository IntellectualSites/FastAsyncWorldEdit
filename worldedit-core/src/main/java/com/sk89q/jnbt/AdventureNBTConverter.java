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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Converts between JNBT and Adventure-NBT classes.
 *
 * @deprecated JNBT is being removed in WE8.
 */
@Deprecated(forRemoval = true)
public class AdventureNBTConverter {

    private static final BiMap<Class<? extends Tag>, LinTagType<?>> TAG_TYPES =
            new ImmutableBiMap.Builder<Class<? extends Tag>, LinTagType<?>>()
                    .put(ByteArrayTag.class, LinTagType.byteArrayTag())
                    .put(ByteTag.class, LinTagType.byteTag())
                    .put(CompoundTag.class, LinTagType.compoundTag())
                    .put(DoubleTag.class, LinTagType.doubleTag())
                    .put(EndTag.class, LinTagType.endTag())
                    .put(FloatTag.class, LinTagType.floatTag())
                    .put(IntArrayTag.class, LinTagType.intArrayTag())
                    .put(IntTag.class, LinTagType.intTag())
                    .put(ListTag.class, LinTagType.listTag())
                    .put(LongArrayTag.class, LinTagType.longArrayTag())
                    .put(LongTag.class, LinTagType.longTag())
                    .put(ShortTag.class, LinTagType.shortTag())
                    .put(StringTag.class, LinTagType.stringTag())
                    .build();

    private static final Map<LinTagType<?>, Function<LinTag, Tag>> CONVERSION;

    static {
        ImmutableMap.Builder<LinTagType<?>, Function<LinTag, Tag>> conversion =
                ImmutableMap.builder();

        for (Map.Entry<Class<? extends Tag>, LinTagType<?>> tag : TAG_TYPES.entrySet()) {
            Constructor<?>[] constructors = tag.getKey().getConstructors();
            for (Constructor<?> c : constructors) {
                if (c.getParameterCount() == 1 && LinTag.class.isAssignableFrom(c.getParameterTypes()[0])) {
                    conversion.put(tag.getValue(), linTag -> {
                        try {
                            return (Tag) c.newInstance(linTag);
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } catch (InvocationTargetException e) {
                            // I assume this is always a RuntimeException since we control the ctor
                            throw (RuntimeException) e.getCause();
                        }
                    });
                    break;
                }
            }
        }

        CONVERSION = conversion.build();
    }

    public static LinTagType<?> getAdventureType(Class<? extends Tag> type) {
        return Objects.requireNonNull(TAG_TYPES.get(type), () -> "Missing entry for " + type);
    }

    public static Class<? extends Tag> getJNBTType(LinTagType<?> type) {
        return Objects.requireNonNull(TAG_TYPES.inverse().get(type), () -> "Missing entry for " + type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <V, LT extends LinTag<? extends V>> Tag<V, LT> toJnbtTag(LT tag) {
        return (Tag<V, LT>) switch (tag.type().id()) {
            case BYTE_ARRAY -> new ByteArrayTag((LinByteArrayTag) tag);
            case BYTE -> new ByteTag((LinByteTag) tag);
            case COMPOUND -> new CompoundTag((LinCompoundTag) tag);
            case DOUBLE -> new DoubleTag((LinDoubleTag) tag);
            case END -> new EndTag();
            case FLOAT -> new FloatTag((LinFloatTag) tag);
            case INT_ARRAY -> new IntArrayTag((LinIntArrayTag) tag);
            case INT -> new IntTag((LinIntTag) tag);
            case LIST -> new ListTag((LinListTag<?>) tag);
            case LONG_ARRAY -> new LongArrayTag((LinLongArrayTag) tag);
            case LONG -> new LongTag((LinLongTag) tag);
            case SHORT -> new ShortTag((LinShortTag) tag);
            case STRING -> new StringTag((LinStringTag) tag);
        };
    }

    private AdventureNBTConverter() {
    }

    public static Tag fromLinBus(LinTag other) {
        if (other == null) {
            return null;
        }
        Function<LinTag, Tag> conversion = CONVERSION.get(other.type());
        if (conversion == null) {
            throw new IllegalArgumentException("Can't convert other of type " + other.getClass().getCanonicalName());
        }
        return conversion.apply(other);
    }

}
