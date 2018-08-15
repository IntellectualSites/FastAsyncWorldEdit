package com.boydti.fawe;

import com.sk89q.jnbt.*;
import com.sk89q.worldedit.world.biome.BaseBiome;

import java.lang.reflect.Field;
import java.util.*;

public class FaweCache {
    /**
     * [ y | z | x ] => index
     */
    public final static short[][][] CACHE_I = new short[256][16][16];
    /**
     * [ y | z | x ] => index
     */
    public final static short[][][] CACHE_J = new short[256][16][16];

    /**
     * [ i | j ] => x
     */
    public final static byte[][] CACHE_X = new byte[16][];
    /**
     * [ i | j ] => y
     */
    public final static short[][] CACHE_Y = new short[16][4096];
    /**
     * [ i | j ] => z
     */
    public final static byte[][] CACHE_Z = new byte[16][];

    /**
     * Immutable biome cache
     */
    public final static BaseBiome[] CACHE_BIOME = new BaseBiome[256];

    public static final BaseBiome getBiome(int id) {
        return CACHE_BIOME[id];
    }

    static {
        for (int i = 0; i < 256; i++) {
            CACHE_BIOME[i] = new BaseBiome(i) {
                @Override
                public void setId(int id) {
                    throw new IllegalStateException("Cannot set id");
                }
            };
        }
        CACHE_X[0] = new byte[4096];
        CACHE_Z[0] = new byte[4096];
        for (int y = 0; y < 16; y++) {
            CACHE_X[y] = CACHE_X[0];
            CACHE_Z[y] = CACHE_Z[0];
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_X[0][j] = (byte) x;
                    CACHE_Z[0][j] = (byte) z;
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    final short i = (short) (y >> 4);
                    final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_I[y][z][x] = i;
                    CACHE_J[y][z][x] = j;
                    CACHE_Y[i][j] = (short) y;
                }
            }
        }
    }

    public static Map<String, Object> asMap(Object... pairs) {
        HashMap<String, Object> map = new HashMap<String, Object>(pairs.length >> 1);
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
            return asTag((Map) value);
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
                        return EndTag.INSTANCE;
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
        Class clazz = null;
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
        Class clazz = null;
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
