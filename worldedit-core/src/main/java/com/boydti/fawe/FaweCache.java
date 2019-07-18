package com.boydti.fawe;

import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FaweCache implements Trimable {
    public static int CHUNK_LAYERS = 16;
    public static int WORLD_HEIGHT = CHUNK_LAYERS << 4;
    public static int WORLD_MAX_Y = WORLD_HEIGHT - 1;


    public static final char[] EMPTY_CHAR_4096 = new char[4096];

    /*
    Palette buffers / cache
     */

    @Override
    public boolean trim(boolean aggressive) {
        BLOCK_TO_PALETTE.clean();
        PALETTE_TO_BLOCK.clean();
        BLOCK_STATES.clean();
        SECTION_BLOCKS.clean();
        PALETTE_CACHE.clean();
        PALETTE_TO_BLOCK_CHAR.clean();

        MUTABLE_VECTOR3.clean();
        MUTABLE_BLOCKVECTOR3.clean();
        return false;
    }

    public static final IterableThreadLocal<int[]> BLOCK_TO_PALETTE = new IterableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            int[] result = new int[BlockTypes.states.length];
            Arrays.fill(result, Integer.MAX_VALUE);
            return result;
        }
    };

    public static final IterableThreadLocal<int[]> PALETTE_TO_BLOCK = new IterableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            return new int[Character.MAX_VALUE + 1];
        }
    };

    public static final IterableThreadLocal<char[]> PALETTE_TO_BLOCK_CHAR = new IterableThreadLocal<char[]>() {
        @Override
        public char[] init() {
            char[] result = new char[Character.MAX_VALUE + 1];
            Arrays.fill(result, Character.MAX_VALUE);
            return result;
        }
    };

    public static final IterableThreadLocal<long[]> BLOCK_STATES = new IterableThreadLocal<long[]>() {
        @Override
        public long[] init() {
            return new long[2048];
        }
    };

    public static final IterableThreadLocal<int[]> SECTION_BLOCKS = new IterableThreadLocal<int[]>() {
        @Override
        public int[] init() {
            return new int[4096];
        }
    };

    /**
     * Holds data for a palette used in a chunk section
     */
    public static final class Palette {
        public int paletteToBlockLength;
        /**
         * Reusable buffer array, MUST check paletteToBlockLength for actual length
         */
        public int[] paletteToBlock;

        public int blockstatesLength;
        /**
         * Reusable buffer array, MUST check blockstatesLength for actual length
         */
        public long[] blockstates;
    }

    private static final IterableThreadLocal<Palette> PALETTE_CACHE = new IterableThreadLocal<Palette>() {
        @Override
        public Palette init() {
            return new Palette();
        }
    };

    /**
     * Convert raw char array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    public static Palette toPalette(int layerOffset, char[] blocks) {
        return toPalette(layerOffset, null, blocks);
    }

    /**
     * Convert raw int array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    public static Palette toPalette(int layerOffset, int[] blocks) {
        return toPalette(layerOffset, blocks, null);
    }

    private static Palette toPalette(int layerOffset, int[] blocksInts, char[] blocksChars) {
        int[] blockToPalette = BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = PALETTE_TO_BLOCK.get();
        long[] blockstates = BLOCK_STATES.get();
        int[] blocksCopy = SECTION_BLOCKS.get();

        int blockIndexStart = layerOffset << 12;
        int blockIndexEnd = blockIndexStart + 4096;
        int num_palette = 0;
        try {
            if (blocksChars != null) {
                for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                    int ordinal = blocksChars[i];
                    int palette = blockToPalette[ordinal];
                    if (palette == Integer.MAX_VALUE) {
//                        BlockState state = BlockTypes.states[ordinal];
                        blockToPalette[ordinal] = palette = num_palette;
                        paletteToBlock[num_palette] = ordinal;
                        num_palette++;
                    }
                    blocksCopy[j] = palette;
                }
            } else if (blocksInts != null) {
                for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                    int ordinal = blocksInts[i];
                    int palette = blockToPalette[ordinal];
                    if (palette == Integer.MAX_VALUE) {
                        BlockState state = BlockTypes.states[ordinal];
                        blockToPalette[ordinal] = palette = num_palette;
                        paletteToBlock[num_palette] = ordinal;
                        num_palette++;
                    }
                    blocksCopy[j] = palette;
                }
            } else {
                throw new IllegalArgumentException();
            }

            for (int i = 0; i < num_palette; i++) {
                blockToPalette[paletteToBlock[i]] = Integer.MAX_VALUE;
            }

            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
            if (num_palette == 1) {
                // Set a value, because minecraft needs it for some  reason
                blockstates[0] = 0;
                blockBitArrayEnd = 1;
            } else {
                BitArray4096 bitArray = new BitArray4096(blockstates, bitsPerEntry);
                bitArray.fromRaw(blocksCopy);
            }

            // Construct palette
            Palette palette = PALETTE_CACHE.get();
            palette.paletteToBlockLength = num_palette;
            palette.paletteToBlock = paletteToBlock;

            palette.blockstatesLength = blockBitArrayEnd;
            palette.blockstates = blockstates;

            return palette;
        } catch (Throwable e) {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * Vector cache
     */

    public static IterableThreadLocal<MutableBlockVector3> MUTABLE_BLOCKVECTOR3 = new IterableThreadLocal<MutableBlockVector3>() {
        @Override
        public MutableBlockVector3 init() {
            return new MutableBlockVector3();
        }
    };

    public static IterableThreadLocal<MutableVector3> MUTABLE_VECTOR3 = new IterableThreadLocal<MutableVector3>() {
        @Override
        public MutableVector3 init() {
            return new MutableVector3();
        }
    };

    /*
    Conversion methods between JNBT tags and raw values
     */
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

    /*
    Thread stuff
     */
    public static ThreadPoolExecutor newBlockingExecutor() {
        int nThreads = Settings.IMP.QUEUE.PARALLEL_THREADS;
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nThreads);
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS, queue
                , Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
