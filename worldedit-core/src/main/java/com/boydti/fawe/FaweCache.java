package com.boydti.fawe;

import com.boydti.fawe.object.collection.CleanableThreadLocal;

import com.sk89q.jnbt.*;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaweCache {
    public static void cleanAll() {
        BLOCK_TO_PALETTE.clean();
        PALETTE_TO_BLOCK.clean();
        BLOCK_STATES.clean();
        SECTION_BLOCKS.clean();
    }

    public static final CleanableThreadLocal<int[]> BLOCK_TO_PALETTE = new CleanableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            int[] result = new int[BlockTypes.states.length];
            Arrays.fill(result, Integer.MAX_VALUE);
            return result;
        }
    };

    public static final CleanableThreadLocal<int[]> PALETTE_TO_BLOCK = new CleanableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            return new int[Character.MAX_VALUE];
        }
    };

    public static final CleanableThreadLocal<long[]> BLOCK_STATES = new CleanableThreadLocal<long[]>() {
        @Override
        public long[] init() {
            return new long[2048];
        }
    };

    public static final CleanableThreadLocal<int[]> SECTION_BLOCKS = new CleanableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            return new int[4096];
        }
    };

    public static Map<String, Object> asMap(Object... pairs) {
        HashMap<String, Object> map = new HashMap<>(pairs.length >> 1);
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public static ShortTag asTag(short value) {
        return new ShortTag(value);
    }

    public static IntTag asTag(int value) {
        return new IntTag(value);
    }

    public static DoubleTag asTag(double value) {
        return new DoubleTag(value);
    }

    public static ByteTag asTag(byte value) {
        return new ByteTag(value);
    }

    public static FloatTag asTag(float value) {
        return new FloatTag(value);
    }

    public static LongTag asTag(long value) {
        return new LongTag(value);
    }

    public static ByteArrayTag asTag(byte[] value) {
        return new ByteArrayTag(value);
    }

    public static IntArrayTag asTag(int[] value) {
        return new IntArrayTag(value);
    }

    public static LongArrayTag asTag(long[] value) {
        return new LongArrayTag(value);
    }

    public static StringTag asTag(String value) {
        return new StringTag(value);
    }

    public static CompoundTag asTag(Map<String, Object> value) {
        HashMap<String, Tag> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Object child = entry.getValue();
            Tag tag = asTag(child);
            map.put(entry.getKey(), tag);
        }
        return new CompoundTag(map);
    }

    public static Tag asTag(Object value) {
        if (value instanceof Integer) {
            return asTag((int) value);
        } else if (value instanceof Short) {
            return asTag((short) value);
        } else if (value instanceof Double) {
            return asTag((double) value);
        } else if (value instanceof Byte) {
            return asTag((byte) value);
        } else if (value instanceof Float) {
            return asTag((float) value);
        } else if (value instanceof Long) {
            return asTag((long) value);
        } else if (value instanceof String) {
            return asTag((String) value);
        } else if (value instanceof Map) {
            return asTag((Map<String, Object>) value);
        } else if (value instanceof Collection) {
            return asTag((Collection) value);
        } else if (value instanceof Object[]) {
            return asTag((Object[]) value);
        } else if (value instanceof byte[]) {
            return asTag((byte[]) value);
        } else if (value instanceof int[]) {
            return asTag((int[]) value);
        } else if (value instanceof long[]) {
            return asTag((long[]) value);
        } else if (value instanceof Tag) {
            return (Tag) value;
        } else if (value instanceof Boolean) {
            return asTag((byte) ((boolean) value ? 1 : 0));
        } else if (value == null) {
            System.out.println("Invalid nbt: " + value);
            return null;
        } else {
            Class<? extends Object> clazz = value.getClass();
            if (clazz.getName().startsWith("com.intellectualcrafters.jnbt")) {
                try {
                    if (clazz.getName().equals("com.intellectualcrafters.jnbt.EndTag")) {
                        return new EndTag();
                    }
                    Field field = clazz.getDeclaredField("value");
                    field.setAccessible(true);
                    return asTag(field.get(value));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Invalid nbt: " + value);
            return null;
        }
    }

    public static ListTag asTag(Object... values) {
        Class<? extends Tag> clazz = null;
        List<Tag> list = new ArrayList<>(values.length);
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) clazz = EndTag.class;
        return new ListTag(clazz, list);
    }

    public static ListTag asTag(Collection values) {
        Class<? extends Tag> clazz = null;
        List<Tag> list = new ArrayList<>(values.size());
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) clazz = EndTag.class;
        return new ListTag(clazz, list);
    }
}
