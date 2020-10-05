package com.boydti.fawe;

import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.beta.implementation.queue.Pool;
import com.boydti.fawe.beta.implementation.queue.QueuePool;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.BitArray;
import com.boydti.fawe.object.collection.BitArrayUnstretched;
import com.boydti.fawe.object.collection.CleanableThreadLocal;
import com.boydti.fawe.object.collection.VariableThreadLocal;
import com.boydti.fawe.object.exception.FaweBlockBagException;
import com.boydti.fawe.object.exception.FaweChunkLoadException;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MathMan;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongArrayTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public enum FaweCache implements Trimable {
    IMP
    ; // singleton

    public final int BLOCKS_PER_LAYER = 4096;
    public final int CHUNK_LAYERS = 16;
    public final int WORLD_HEIGHT = CHUNK_LAYERS << 4;
    public final int WORLD_MAX_Y = WORLD_HEIGHT - 1;

    public final char[] EMPTY_CHAR_4096 = new char[4096];

    private final IdentityHashMap<Class<? extends IChunkSet>, Pool<? extends IChunkSet>> REGISTERED_POOLS = new IdentityHashMap<>();

    /*
    Palette buffers / cache
     */

    @Override
    public synchronized boolean trim(boolean aggressive) {
        if (aggressive) {
            CleanableThreadLocal.cleanAll();
        } else {
            CHUNK_FLAG.clean();
            BYTE_BUFFER_8192.clean();
            BLOCK_TO_PALETTE.clean();
            PALETTE_TO_BLOCK.clean();
            BLOCK_STATES.clean();
            SECTION_BLOCKS.clean();
            PALETTE_CACHE.clean();
            PALETTE_TO_BLOCK_CHAR.clean();
            INDEX_STORE.clean();

            MUTABLE_VECTOR3.clean();
            MUTABLE_BLOCKVECTOR3.clean();
            SECTION_BITS_TO_CHAR.clean();
        }
        for (Entry<Class<? extends IChunkSet>, Pool<? extends IChunkSet>> entry : REGISTERED_POOLS.entrySet()) {
            Pool<? extends IChunkSet> pool = entry.getValue();
            pool.clear();
        }

        return false;
    }

    public synchronized <T extends IChunkSet> Pool<T> registerPool(Class<T> clazz, Supplier<T> cache, boolean buffer) {
        checkNotNull(cache);
        Pool<T> pool;
        if (buffer) {
            pool = new QueuePool<>(cache);
        } else {
            pool = cache::get;
        }
        Pool<? extends IChunkSet> previous = REGISTERED_POOLS.putIfAbsent(clazz, pool);
        if (previous != null) {
            throw new IllegalStateException("Previous key");
        }
        return pool;
    }

    public <T, V> LoadingCache<T, V> createCache(Supplier<V> withInitial) {
        return CacheBuilder.newBuilder().build(new CacheLoader<T, V>() {
            @Override
            public V load(@NotNull T key) {
                return withInitial.get();
            }
        });
    }

    public <T, V> LoadingCache<T, V> createCache(Function<T, V> withInitial) {
        return CacheBuilder.newBuilder().build(new CacheLoader<T, V>() {
            @Override
            public V load(@NotNull T key) {
                return withInitial.apply(key);
            }
        });
    }

    /*
    Exceptions
     */
    public static final FaweChunkLoadException CHUNK = new FaweChunkLoadException();
    public static final FaweBlockBagException BLOCK_BAG = new FaweBlockBagException();
    public static final FaweException MANUAL = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.manual"));
    public static final FaweException NO_REGION = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.no.region"));
    public static final FaweException OUTSIDE_REGION = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.outside.region"));
    public static final FaweException MAX_CHECKS = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.checks"));
    public static final FaweException MAX_CHANGES = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.changes"));
    public static final FaweException LOW_MEMORY = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.low.memory"));
    public static final FaweException MAX_ENTITIES = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.entities"));
    public static final FaweException MAX_TILES = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.tiles"));
    public static final FaweException MAX_ITERATIONS = new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.iterations"));

    /*
    thread cache
     */
    public final CleanableThreadLocal<AtomicBoolean> CHUNK_FLAG = new CleanableThreadLocal<>(AtomicBoolean::new); // resets to false

    public final CleanableThreadLocal<long[]> LONG_BUFFER_1024 = new CleanableThreadLocal<>(() -> new long[1024]);

    public final CleanableThreadLocal<byte[]> BYTE_BUFFER_8192 = new CleanableThreadLocal<>(() -> new byte[8192]);

    public final VariableThreadLocal BYTE_BUFFER_VAR = new VariableThreadLocal();

    public final CleanableThreadLocal<int[]> BLOCK_TO_PALETTE = new CleanableThreadLocal<>(() -> {
        int[] result = new int[BlockTypesCache.states.length];
        Arrays.fill(result, Integer.MAX_VALUE);
        return result;
    });

    public final CleanableThreadLocal<char[]> SECTION_BITS_TO_CHAR = new CleanableThreadLocal<>(() -> new char[4096]);

    public final CleanableThreadLocal<int[]> PALETTE_TO_BLOCK = new CleanableThreadLocal<>(() -> new int[Character.MAX_VALUE + 1]);

    public final CleanableThreadLocal<char[]> PALETTE_TO_BLOCK_CHAR = new CleanableThreadLocal<>(
        () -> new char[Character.MAX_VALUE + 1], a -> {
            Arrays.fill(a, Character.MAX_VALUE);
        }
    );

    public final CleanableThreadLocal<long[]> BLOCK_STATES = new CleanableThreadLocal<>(() -> new long[2048]);

    public final CleanableThreadLocal<int[]> SECTION_BLOCKS = new CleanableThreadLocal<>(() -> new int[4096]);

    public final CleanableThreadLocal<int[]> INDEX_STORE = new CleanableThreadLocal<>(() -> new int[256]);

    public final CleanableThreadLocal<int[]> HEIGHT_STORE = new CleanableThreadLocal<>(() -> new int[256]);

    /**
     * Holds data for a palette used in a chunk section
     */
    public static final class Palette {
        public int bitsPerEntry;

        public int paletteToBlockLength;
        /**
         * Reusable buffer array, MUST check paletteToBlockLength for actual length
         */
        public int[] paletteToBlock;

        public int blockStatesLength;
        /**
         * Reusable buffer array, MUST check blockStatesLength for actual length
         */
        public long[] blockStates;
    }

    private final CleanableThreadLocal<Palette> PALETTE_CACHE = new CleanableThreadLocal<>(Palette::new);

    /**
     * Convert raw char array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    public Palette toPalette(int layerOffset, char[] blocks) {
        return toPalette(layerOffset, null, blocks);
    }

    /**
     * Convert raw int array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    public Palette toPalette(int layerOffset, int[] blocks) {
        return toPalette(layerOffset, blocks, null);
    }

    private Palette toPalette(int layerOffset, int[] blocksInts, char[] blocksChars) {
        int[] blockToPalette = BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = PALETTE_TO_BLOCK.get();
        long[] blockStates = BLOCK_STATES.get();
        int[] blocksCopy = SECTION_BLOCKS.get();

        try {
            int num_palette = 0;
            int blockIndexStart = layerOffset << 12;
            int blockIndexEnd = blockIndexStart + 4096;
            if (blocksChars != null) {
                for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                    int ordinal = blocksChars[i];
                    int palette = blockToPalette[ordinal];
                    if (palette == Integer.MAX_VALUE) {
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
//                        BlockState state = BlockTypesCache.states[ordinal];
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
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
            }
            int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
            if (num_palette == 1) {
                // Set a value, because minecraft needs it for some  reason
                blockStates[0] = 0;
                blockBitArrayEnd = 1;
            } else {
                BitArray bitArray = new BitArray(bitsPerEntry, 4096, blockStates);
                bitArray.fromRaw(blocksCopy);
            }

            // Construct palette
            Palette palette = PALETTE_CACHE.get();
            palette.bitsPerEntry = bitsPerEntry;
            palette.paletteToBlockLength = num_palette;
            palette.paletteToBlock = paletteToBlock;

            palette.blockStatesLength = blockBitArrayEnd;
            palette.blockStates = blockStates;

            return palette;
        } catch (Throwable e) {
            e.printStackTrace();
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    /**
     * Convert raw int array to unstretched palette (1.16)
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    public Palette toPaletteUnstretched(int layerOffset, char[] blocks) {
        return toPaletteUnstretched(layerOffset, null, blocks);
    }

    /**
     * Convert raw int array to unstretched palette (1.16)
     * @return palette
     */
    public Palette toPaletteUnstretched(int layerOffset, int[] blocks) {
        return toPaletteUnstretched(layerOffset, blocks, null);
    }

    private Palette toPaletteUnstretched(int layerOffset, int[] blocksInts, char[] blocksChars) {
        int[] blockToPalette = BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = PALETTE_TO_BLOCK.get();
        long[] blockStates = BLOCK_STATES.get();
        int[] blocksCopy = SECTION_BLOCKS.get();

        try {
            int num_palette = 0;
            int blockIndexStart = layerOffset << 12;
            int blockIndexEnd = blockIndexStart + 4096;
            if (blocksChars != null) {
                for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                    int ordinal = blocksChars[i];
                    int palette = blockToPalette[ordinal];
                    if (palette == Integer.MAX_VALUE) {
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
                        //                        BlockState state = BlockTypesCache.states[ordinal];
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
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
            }
            int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntry);
            int blockBitArrayEnd = MathMan.ceilZero((float) 4096 / blocksPerLong);
            if (num_palette == 1) {
                // Set a value, because minecraft needs it for some  reason
                blockStates[0] = 0;
                blockBitArrayEnd = 1;
            } else {
                BitArrayUnstretched bitArray = new BitArrayUnstretched(bitsPerEntry, 4096, blockStates);
                bitArray.fromRaw(blocksCopy);
            }

            // Construct palette
            Palette palette = PALETTE_CACHE.get();
            palette.bitsPerEntry = bitsPerEntry;
            palette.paletteToBlockLength = num_palette;
            palette.paletteToBlock = paletteToBlock;

            palette.blockStatesLength = blockBitArrayEnd;
            palette.blockStates = blockStates;

            return palette;
        } catch (Throwable e) {
            e.printStackTrace();
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    /*
     * Vector cache
     */

    public CleanableThreadLocal<MutableBlockVector3> MUTABLE_BLOCKVECTOR3 = new CleanableThreadLocal<>(MutableBlockVector3::new);

    public CleanableThreadLocal<MutableVector3> MUTABLE_VECTOR3 = new CleanableThreadLocal<MutableVector3>(MutableVector3::new) {
        @Override
        public MutableVector3 init() {
            return new MutableVector3();
        }
    };

    /*
    Conversion methods between JNBT tags and raw values
     */
    public Map<String, Object> asMap(Object... pairs) {
        HashMap<String, Object> map = new HashMap<>(pairs.length >> 1);
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public ShortTag asTag(short value) {
        return new ShortTag(value);
    }

    public IntTag asTag(int value) {
        return new IntTag(value);
    }

    public DoubleTag asTag(double value) {
        return new DoubleTag(value);
    }

    public ByteTag asTag(byte value) {
        return new ByteTag(value);
    }

    public FloatTag asTag(float value) {
        return new FloatTag(value);
    }

    public LongTag asTag(long value) {
        return new LongTag(value);
    }

    public ByteArrayTag asTag(byte[] value) {
        return new ByteArrayTag(value);
    }

    public IntArrayTag asTag(int[] value) {
        return new IntArrayTag(value);
    }

    public LongArrayTag asTag(long[] value) {
        return new LongArrayTag(value);
    }

    public StringTag asTag(String value) {
        return new StringTag(value);
    }

    public CompoundTag asTag(Map<String, Object> value) {
        HashMap<String, Tag> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Object child = entry.getValue();
            Tag tag = asTag(child);
            map.put(entry.getKey(), tag);
        }
        return new CompoundTag(map);
    }

    public Tag asTag(Object value) {
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
        }
        System.out.println("Invalid nbt: " + value);
        return null;
    }

    public ListTag asTag(Object... values) {
        Class<? extends Tag> clazz = null;
        List<Tag> list = new ArrayList<>(values.length);
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) {
            clazz = EndTag.class;
        }
        return new ListTag(clazz, list);
    }

    public ListTag asTag(Collection values) {
        Class<? extends Tag> clazz = null;
        List<Tag> list = new ArrayList<>(values.size());
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) {
            clazz = EndTag.class;
        }
        return new ListTag(clazz, list);
    }

    /*
    Thread stuff
     */
    public ThreadPoolExecutor newBlockingExecutor() {
        int nThreads = Settings.IMP.QUEUE.PARALLEL_THREADS;
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nThreads);
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS, queue,
                                      Executors.defaultThreadFactory(),
                                      new ThreadPoolExecutor.CallerRunsPolicy()) {
            protected void afterExecute(Runnable r, Throwable t) {
                try {
                    super.afterExecute(r, t);
                    if (t == null && r instanceof Future<?>) {
                        try {
                            Future<?> future = (Future<?>) r;
                            if (future.isDone()) {
                                future.get();
                            }
                        } catch (CancellationException ce) {
                            t = ce;
                        } catch (ExecutionException ee) {
                            t = ee.getCause();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (t != null) {
                        t.printStackTrace();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
