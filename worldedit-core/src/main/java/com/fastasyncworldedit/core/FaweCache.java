package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.filter.block.DataArrayFilterBlock;
import com.fastasyncworldedit.core.internal.exception.FaweBlockBagException;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.internal.exception.FaweException.Type;
import com.fastasyncworldedit.core.math.BitArray;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.Pool;
import com.fastasyncworldedit.core.queue.Trimable;
import com.fastasyncworldedit.core.queue.implementation.QueuePool;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.CleanableThreadLocal;
import com.fastasyncworldedit.core.util.task.FaweBasicThreadFactory;
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
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public enum FaweCache implements Trimable {
    /**
     * @since 2.0.0
     */
    INSTANCE;

    /**
     * @deprecated Use {@link #INSTANCE} to get an instance.
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static final FaweCache IMP = INSTANCE;

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public final int BLOCKS_PER_LAYER = 4096;

    /**
     * @since TODO
     */
    public final DataArray EMPTY_DATA = DataArray.EMPTY;

    private final IdentityHashMap<Class<? extends IChunkSet>, Pool<? extends IChunkSet>> REGISTERED_POOLS = new IdentityHashMap<>();

    /*
    Palette buffers / cache
     */

    @Override
    public synchronized boolean trim(boolean aggressive) {
        CHUNK_FLAG.clean();
        BYTE_BUFFER_8192.clean();
        BLOCK_TO_PALETTE.clean();
        PALETTE_TO_BLOCK.clean();
        BLOCK_STATES.clean();
        SECTION_BLOCKS.clean();
        PALETTE_CACHE.clean();
        PALETTE_TO_BLOCK_CHAR.clean();
        PALETTE_TO_BLOCK_INT.clean();
        INDEX_STORE.clean();

        MUTABLE_VECTOR3.clean();
        MUTABLE_BLOCKVECTOR3.clean();
        SECTION_BITS_TO_CHAR.clean();
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
        return CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public V load(@Nonnull T key) {
                return withInitial.get();
            }
        });
    }

    public <T, V> LoadingCache<T, V> createCache(Function<T, V> withInitial) {
        return CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public V load(@Nonnull T key) {
                return withInitial.apply(key);
            }
        });
    }

    /**
     * Create a new cache aimed to act as a thread-cache that is safe to the main server thread. If the method is called away
     * from the main thread, it will return a {@link Function} referencing the {@link LoadingCache} returned by
     * {@link FaweCache#createCache(Supplier)}. If it is called from the main thread, it will return a {@link Function} that
     * will always return the result of the given {@link Supplier}. It is designed to prevent issues caused by
     * internally-mutable and resettable classes such as {@link DataArrayFilterBlock}
     * from causing issues when used in edits on the main thread.
     * <p>
     * This method is designed for specific internal use.
     *
     * @param withInitial The supplier used to determine the initial value if a thread cache is created, else to provide a new
     *                    instance of the class being cached if on the main thread.
     * @return a {@link Function} referencing a cache, or the given {@link Supplier}
     * @since 2.4.0
     */
    public <V> LongFunction<V> createMainThreadSafeCache(Supplier<V> withInitial) {
        return new LongFunction<>() {
            private final LoadingCache<Long, V> loadingCache = Fawe.isMainThread() ? null : FaweCache.INSTANCE.createCache(
                    withInitial);

            @Override
            public V apply(final long input) {
                return loadingCache != null ? loadingCache.getUnchecked(input) : withInitial.get();
            }
        };
    }

    /*
    Exceptions
     */
    public static final FaweBlockBagException BLOCK_BAG = new FaweBlockBagException();
    public static final FaweException MANUAL = new FaweException(
            Caption.of("fawe.cancel.reason.manual"),
            Type.MANUAL,
            false
    );
    public static final FaweException NO_REGION = new FaweException(
            Caption.of("fawe.cancel.reason.no.region"),
            Type.NO_REGION,
            false
    );
    public static final FaweException OUTSIDE_REGION = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.outside.region"),
            Type.OUTSIDE_REGION,
            true
    );
    public static final FaweException OUTSIDE_SAFE_REGION = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.outside.safe.region"),
            Type.OUTSIDE_REGION
    );
    public static final FaweException MAX_CHECKS = new FaweException(
            Caption.of("fawe.cancel.reason.max.checks"),
            Type.MAX_CHECKS,
            true
    );
    public static final FaweException MAX_FAILS = new FaweException(
            Caption.of("fawe.cancel.reason.max.fails"),
            Type.MAX_CHECKS,
            true
    );
    public static final FaweException MAX_CHANGES = new FaweException(
            Caption.of("fawe.cancel.reason.max.changes"),
            Type.MAX_CHANGES,
            false
    );
    public static final FaweException LOW_MEMORY = new FaweException(
            Caption.of("fawe.cancel.reason.low.memory"),
            Type.LOW_MEMORY,
            false
    );
    public static final FaweException MAX_ENTITIES = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.max.entities"),
            Type.MAX_ENTITIES,
            true
    );
    public static final FaweException MAX_TILES = new FaweException(Caption.of(
            "fawe.cancel.reason.max.tiles",
            Type.MAX_TILES,
            true
    ));
    public static final FaweException MAX_ITERATIONS = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.max.iterations"),
            Type.MAX_ITERATIONS,
            true
    );
    public static final FaweException PLAYER_ONLY = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.player-only"),
            Type.PLAYER_ONLY,
            false
    );
    public static final FaweException ACTOR_REQUIRED = new FaweException(
            Caption.of(
                    "fawe.cancel.reason.actor-required"),
            Type.ACTOR_REQUIRED,
            false
    );

    /*
    thread cache
     */
    public final CleanableThreadLocal<AtomicBoolean> CHUNK_FLAG = new CleanableThreadLocal<>(AtomicBoolean::new); // resets to false


    public final CleanableThreadLocal<byte[]> BYTE_BUFFER_8192 = new CleanableThreadLocal<>(() -> new byte[8192]);

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
    });

    public final CleanableThreadLocal<int[]> PALETTE_TO_BLOCK_INT = new CleanableThreadLocal<>(
            () -> new int[Character.MAX_VALUE + 1], a -> {
        Arrays.fill(a, Integer.MAX_VALUE);
    });

    public final CleanableThreadLocal<long[]> BLOCK_STATES = new CleanableThreadLocal<>(() -> new long[2048]);

    public final CleanableThreadLocal<int[]> SECTION_BLOCKS = new CleanableThreadLocal<>(() -> new int[4096]);

    public final CleanableThreadLocal<int[]> INDEX_STORE = new CleanableThreadLocal<>(() -> new int[256]);

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
     *
     * @return palette
     */
    public Palette toPalette(int layerOffset, DataArray blocks) {
        int[] blockToPalette = BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = PALETTE_TO_BLOCK.get();
        long[] blockStates = BLOCK_STATES.get();
        int[] blocksCopy = SECTION_BLOCKS.get();

        try {
            int num_palette = 0;
            int blockIndexStart = layerOffset << 12;
            int blockIndexEnd = blockIndexStart + 4096;
            for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                int ordinal = blocks.getAt(i);
                int palette = blockToPalette[ordinal];
                if (palette == Integer.MAX_VALUE) {
                    blockToPalette[ordinal] = palette = num_palette;
                    paletteToBlock[num_palette] = ordinal;
                    num_palette++;
                }
                blocksCopy[j] = palette;
            }

            for (int i = 0; i < num_palette; i++) {
                blockToPalette[paletteToBlock[i]] = Integer.MAX_VALUE;
            }

            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.settings().PROTOCOL_SUPPORT_FIX || num_palette != 1) {
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
     *
     * @return palette
     */
    public Palette toPaletteUnstretched(int layerOffset, DataArray blocks) {
        int[] blockToPalette = BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = PALETTE_TO_BLOCK.get();
        long[] blockStates = BLOCK_STATES.get();
        int[] blocksCopy = SECTION_BLOCKS.get();

        try {
            int num_palette = 0;
            int blockIndexStart = layerOffset << 12;
            int blockIndexEnd = blockIndexStart + 4096;
            for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                int ordinal = blocks.getAt(i);
                int palette = blockToPalette[ordinal];
                if (palette == Integer.MAX_VALUE) {
                    blockToPalette[ordinal] = palette = num_palette;
                    paletteToBlock[num_palette] = ordinal;
                    num_palette++;
                }
                blocksCopy[j] = palette;
            }

            for (int i = 0; i < num_palette; i++) {
                blockToPalette[paletteToBlock[i]] = Integer.MAX_VALUE;
            }

            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.settings().PROTOCOL_SUPPORT_FIX || num_palette != 1) {
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
        HashMap<String, Tag<?, ?>> map = new HashMap<>();
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
        LOGGER.error("Invalid nbt: {}", value);
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

    /**
     * Create a new blocking executor with default name and FaweCache logger
     *
     * @return new blocking executor
     */
    public ThreadPoolExecutor newBlockingExecutor() {
        return newBlockingExecutor("FAWE Blocking Executor - %d");
    }

    /**
     * Create a new blocking executor with specified name and FaweCache logger
     *
     * @return new blocking executor
     * @since 2.9.0
     */
    public ThreadPoolExecutor newBlockingExecutor(String name) {
        return newBlockingExecutor(name, LOGGER);
    }

    /**
     * Create a new blocking executor with specified name and logger
     *
     * @return new blocking executor
     * @since 2.9.0
     */
    public ThreadPoolExecutor newBlockingExecutor(String name, Logger logger) {
        int nThreads = Settings.settings().QUEUE.PARALLEL_THREADS;
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        return new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                new FaweBasicThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ) {

            // Array for lazy avoidance of concurrent modification exceptions and needless overcomplication of code (synchronisation is
            // not very important)
            private final boolean[] faweExceptionReasonsUsed = new boolean[FaweException.Type.values().length];
            private int lastException = Integer.MIN_VALUE;
            private int count = 0;

            protected synchronized void afterExecute(Runnable runnable, Throwable throwable) {
                super.afterExecute(runnable, throwable);
                if (throwable == null && runnable instanceof Future<?>) {
                    try {
                        Future<?> future = (Future<?>) runnable;
                        if (future.isDone()) {
                            future.get();
                        }
                    } catch (CancellationException ce) {
                        throwable = ce;
                    } catch (ExecutionException ee) {
                        throwable = ee.getCause();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (throwable != null) {
                    if (throwable instanceof FaweException) {
                        handleFaweException((FaweException) throwable);
                    } else if (throwable.getCause() instanceof FaweException) {
                        handleFaweException((FaweException) throwable.getCause());
                    } else {
                        int hash = throwable.getMessage() != null ? throwable.getMessage().hashCode() : 0;
                        if (hash != lastException) {
                            lastException = hash;
                            logger.catching(throwable);
                            count = 0;
                        } else if (count < Settings.settings().QUEUE.PARALLEL_THREADS) {
                            logger.warn(throwable.getMessage());
                            count++;
                        }
                    }
                }
            }

            private void handleFaweException(FaweException e) {
                FaweException.Type type = e.getType();
                if (e.getType() == FaweException.Type.OTHER) {
                    logger.catching(e);
                } else if (!faweExceptionReasonsUsed[type.ordinal()]) {
                    faweExceptionReasonsUsed[type.ordinal()] = true;
                    logger.warn("FaweException: " + e.getMessage());
                }
            }
        };
    }
}
