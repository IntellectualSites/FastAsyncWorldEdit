package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.util.CachedTextureUtil;
import com.fastasyncworldedit.core.util.CleanTextureUtil;
import com.fastasyncworldedit.core.util.FaweTimer;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.util.RandomTextureUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.fastasyncworldedit.core.util.WEManager;
import com.fastasyncworldedit.core.util.task.KeyQueuedExecutorService;
import com.fastasyncworldedit.core.util.task.UUIDKeyQueuedThreadFactory;
import com.github.luben.zstd.Zstd;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import net.jpountz.lz4.LZ4Factory;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.NotificationEmitter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * [ WorldEdit action]
 * |
 * \|/
 * [ EditSession ] - The change is processed (area restrictions, change limit, block type)
 * |
 * \|/
 * [Block change] - A block change from some location
 * |
 * \|/
 * [ Set Queue ] - The SetQueue manages the implementation specific queue
 * |
 * \|/
 * [ Fawe Queue] - A queue of chunks - check if the queue has the chunk for a change
 * |
 * \|/
 * [ Fawe Chunk Implementation ] - Otherwise create a new FaweChunk object which is a wrapper around the Chunk object
 * |
 * \|/
 * [ Execution ] - When done, the queue then sets the blocks for the chunk, performs lighting updates and sends the chunk packet to the clients
 * <p>
 * Why it's faster:
 * - The chunk is modified directly rather than through the API
 * \ Removes some overhead, and means some processing can be done async
 * - Lighting updates are performed on the chunk level rather than for every block
 * \ e.g., A blob of stone: only the visible blocks need to have the lighting calculated
 * - Block changes are sent with a chunk packet
 * \ A chunk packet is generally quicker to create and smaller for large world edits
 * - No physics updates
 * \ Physics updates are slow, and are usually performed on each block
 * - Block data shortcuts
 * \ Some known blocks don't need to have the data set or accessed (e.g., air is never going to have data)
 * - Remove redundant extents
 * \ Up to 11 layers of extents can be removed
 * - History bypassing
 * \ FastMode bypasses history and means blocks in the world don't need to be checked and recorded
 */
public class Fawe {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static Fawe instance;

    /**
     * The ticks-per-second timer.
     */
    private final FaweTimer timer;
    /**
     * The platform specific implementation.
     */
    private final IFawe implementation;
    private final KeyQueuedExecutorService<UUID> uuidKeyQueuedExecutorService;
    private FaweVersion version;
    private TextureUtil textures;
    private QueueHandler queueHandler;
    private Thread thread;

    private Fawe(final IFawe implementation) {
        instance = this;
        this.implementation = implementation;
        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        this.setupConfigs();
        FaweLimit.MAX.CONFIRM_LARGE =
                Settings.settings().LIMITS.get("default").CONFIRM_LARGE || Settings.settings().GENERAL.LIMIT_UNLIMITED_CONFIRMS;
        TaskManager.IMP = this.implementation.getTaskManager();

        TaskManager.taskManager().async(() -> {
            MainUtil.deleteOlder(
                    MainUtil.getFile(this.implementation
                            .getDirectory(), Settings.settings().PATHS.HISTORY),
                    TimeUnit.DAYS.toMillis(Settings.settings().HISTORY.DELETE_AFTER_DAYS),
                    false
            );
            MainUtil.deleteOlder(
                    MainUtil.getFile(this.implementation
                            .getDirectory(), Settings.settings().PATHS.CLIPBOARD),
                    TimeUnit.DAYS.toMillis(Settings.settings().CLIPBOARD.DELETE_AFTER_DAYS),
                    false
            );
        });

        /*
         * Instance independent stuff
         */
        this.setupMemoryListener();
        this.timer = new FaweTimer();

        // Delayed worldedit setup
        TaskManager.taskManager().later(() -> {
            try {
                WEManager.weManager().addManagers(Fawe.this.implementation.getMaskManagers());
            } catch (Throwable ignored) {
            }
        }, 0);

        TaskManager.taskManager().repeat(timer, 1);
        uuidKeyQueuedExecutorService = new KeyQueuedExecutorService<>(new ThreadPoolExecutor(
                1,
                Settings.settings().QUEUE.PARALLEL_THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new UUIDKeyQueuedThreadFactory()
        ));
    }

    /**
     * Get the implementation specific class.
     * @deprecated use {@link #platform()}
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static <T extends IFawe> T imp() {
        return instance != null ? (T) instance.implementation : null;
    }

    /**
     * Get the implementation specific class.
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    public static <T extends IFawe> T platform() {
        return instance != null ? (T) instance.implementation : null;
    }


    /**
     * Get the implementation independent class.
     * @deprecated use {@link #instance()}
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static Fawe get() {
        return instance;
    }

    /**
     * Get the implementation independent class.
     */
    public static Fawe instance() {
        return instance;
    }

    /**
     * This method is not for public use. If you have to ask what it does then you shouldn't be using it.
     */
    public static void set(final IFawe implementation) throws InstanceAlreadyExistsException, IllegalArgumentException {
        if (instance != null) {
            throw new InstanceAlreadyExistsException("FAWE has already been initialized with: " + instance.implementation);
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation may not be null.");
        }
        instance = new Fawe(implementation);
    }

    public static void setupInjector() {
        // Check Base OS Arch for Mismatching Architectures
        boolean x86OS = System.getProperty("sun.arch.data.model").contains("32");
        boolean x86JVM = System.getProperty("os.arch").contains("32");
        if (x86OS != x86JVM) {
            LOGGER.info("You are running 32-bit Java on a 64-bit machine. Please upgrade to 64-bit Java.");
        }
    }

    public static boolean isMainThread() {
        return instance == null || instance.thread == Thread.currentThread();
    }

    /**
     * Non-api. Handles an input FAWE exception if not already handled, given the input boolean array.
     * Looks at the {@link FaweException.Type} and decides what to do (rethrows if we want to attempt to show the error to the
     * player, outputs to console where necessary).
     *
     * @param faweExceptionReasonsUsed boolean array that should be cached where this method is called from of length {@code
     *                                 FaweException.Type.values().length}
     * @param e                        {@link FaweException} to handle
     * @param logger                   {@link Logger} of the calling class
     */
    public static void handleFaweException(
            boolean[] faweExceptionReasonsUsed,
            FaweException e,
            final Logger logger
    ) {
        FaweException.Type type = e.getType();
        switch (type) {
            case OTHER:
                logger.catching(e);
                throw e;
            case PLAYER_ONLY:
            case ACTOR_REQUIRED:
            case LOW_MEMORY:
                if (!faweExceptionReasonsUsed[type.ordinal()]) {
                    logger.warn("FaweException: " + e.getMessage());
                    faweExceptionReasonsUsed[type.ordinal()] = true;
                    throw e;
                }
            case MAX_TILES:
            case NO_REGION:
            case MAX_CHECKS:
            case MAX_CHANGES:
            case MAX_ENTITIES:
            case MAX_ITERATIONS:
            case OUTSIDE_REGION:
            case CLIPBOARD:
                if (!faweExceptionReasonsUsed[type.ordinal()]) {
                    faweExceptionReasonsUsed[type.ordinal()] = true;
                    throw e;
                } else {
                    return;
                }
            default:
                if (!faweExceptionReasonsUsed[type.ordinal()]) {
                    faweExceptionReasonsUsed[type.ordinal()] = true;
                    logger.warn("FaweException: " + e.getMessage());
                }
        }
    }

    public void onDisable() {
        if (platform().getPreloader(false) != null) {
            platform().getPreloader(false).cancel();
        }
    }

    public QueueHandler getQueueHandler() {
        if (queueHandler == null) {
            synchronized (this) {
                if (queueHandler == null) {
                    queueHandler = implementation.getQueueHandler();
                }
            }
        }
        return queueHandler;
    }

    public TextureUtil getCachedTextureUtil(boolean randomize, int min, int max) {
        // TODO NOT IMPLEMENTED - optimize this by caching the default true/0/100 texture util
        TextureUtil tu = getTextureUtil();
        try {
            tu = min == 0 && max == 100 ? tu : new CleanTextureUtil(tu, min, max);
            tu = randomize ? new RandomTextureUtil(tu) : new CachedTextureUtil(tu);
        } catch (FileNotFoundException neverHappens) {
            neverHappens.printStackTrace();
        }
        return tu;
    }

    public TextureUtil getTextureUtil() {
        TextureUtil tmp = textures;
        if (tmp == null) {
            synchronized (this) {
                tmp = textures;
                if (tmp == null) {
                    try {
                        textures = tmp = new TextureUtil();
                        tmp.loadModTextures();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * Gets the TPS monitor.
     */
    public FaweTimer getTimer() {
        return timer;
    }

    /**
     * The FAWE version.
     *
     * @return FaweVersion
     */
    @Nullable
    public FaweVersion getVersion() {
        return version;
    }

    public double getTPS() {
        return timer.getTPS();
    }

    public void setupConfigs() {
        MainUtil.copyFile(MainUtil.getJarFile(), "lang/strings.json", null);
        // Setting up config.yml
        File file = new File(this.implementation.getDirectory(), "config.yml");
        Settings.settings().PLATFORM = implementation.getPlatform().replace("\"", "");
        try (InputStream stream = getClass().getResourceAsStream("/fawe.properties");
             BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String versionString = br.readLine();
            String commitString = br.readLine();
            String dateString = br.readLine();
            br.close();
            this.version = FaweVersion.tryParse(versionString, commitString, dateString);
            Settings.settings().DATE = new Date(100 + version.year, version.month, version.day).toString();
            Settings.settings().BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
            Settings.settings().COMMIT = "https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignored) {
        }
        try {
            Settings.settings().reload(file);
        } catch (Throwable e) {
            LOGGER.error("Failed to load config.", e);
        }
        Settings.settings().QUEUE.TARGET_SIZE = Math.max(
                Settings.settings().QUEUE.TARGET_SIZE,
                Settings.settings().QUEUE.PARALLEL_THREADS
        );
        if (Settings.settings().QUEUE.TARGET_SIZE < 4 * Settings.settings().QUEUE.PARALLEL_THREADS) {
            LOGGER.error(
                    "queue.target-size is {}, and queue.parallel_threads is {}. It is HIGHLY recommended that queue" +
                            ".target-size be at least four times queue.parallel-threads or greater.",
                    Settings.settings().QUEUE.TARGET_SIZE,
                    Settings.settings().QUEUE.PARALLEL_THREADS
            );
        }
        if (Settings.settings().HISTORY.DELETE_DISK_ON_LOGOUT && Settings.settings().HISTORY.USE_DATABASE) {
            LOGGER.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            LOGGER.warn("!!!                                                                !!!");
            LOGGER.warn("!!!    Using history database whilst deleting disk history!        !!!");
            LOGGER.warn("!!!    You will not be able to rollback edits after a user logs    !!!");
            LOGGER.warn("!!!    out, recommended to disable delete-disk-on-logout if you    !!!");
            LOGGER.warn("!!!    you want to have full history rollback functionality.       !!!");
            LOGGER.warn("!!!    Disable use-database if you do not need to have rollback    !!!");
            LOGGER.warn("!!!    functionality and wish to disable this warning.             !!!");
            LOGGER.warn("!!!                                                                !!!");
            LOGGER.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        try {
            byte[] in = new byte[0];
            byte[] compressed = LZ4Factory.fastestJavaInstance().fastCompressor().compress(in);
            byte[] ob = new byte[100];
            assert (LZ4Factory.fastestJavaInstance().fastDecompressor().decompress(ob, compressed) == 0);
            LOGGER.info("LZ4 Compression Binding loaded successfully");
        } catch (Throwable e) {
            LOGGER.error("LZ4 Compression Binding Not Found.\n"
                    + "FAWE will still work but compression will be slower.", e);
        }
        try {
            byte[] in = new byte[0];
            byte[] compressed = Zstd.compress(in);
            byte[] ob = new byte[100];
            assert (Zstd.decompress(ob, compressed) == 0);
            LOGGER.info("ZSTD Compression Binding loaded successfully");
        } catch (Throwable e) {
            if (Settings.settings().CLIPBOARD.COMPRESSION_LEVEL > 6 || Settings.settings().HISTORY.COMPRESSION_LEVEL > 6) {
                Settings.settings().CLIPBOARD.COMPRESSION_LEVEL = Math.min(6, Settings.settings().CLIPBOARD.COMPRESSION_LEVEL);
                Settings.settings().HISTORY.COMPRESSION_LEVEL = Math.min(6, Settings.settings().HISTORY.COMPRESSION_LEVEL);
                LOGGER.error("ZSTD Compression Binding Not Found.\n"
                        + "FAWE will still work but compression won't work as well.", e);
            }
        }
        Settings.settings().save(file);
    }

    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    private void setupMemoryListener() {
        if (Settings.settings().MAX_MEMORY_PERCENT < 1 || Settings.settings().MAX_MEMORY_PERCENT > 99) {
            return;
        }
        try {
            final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            final NotificationEmitter ne = (NotificationEmitter) memBean;

            ne.addNotificationListener((notification, handback) -> {
                final long heapSize = Runtime.getRuntime().totalMemory();
                final long heapMaxSize = Runtime.getRuntime().maxMemory();
                if (heapSize < heapMaxSize) {
                    return;
                }
                MemUtil.memoryLimitedTask();
            }, null, null);

            final List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
            for (final MemoryPoolMXBean mp : memPools) {
                if (mp.isUsageThresholdSupported()) {
                    final MemoryUsage mu = mp.getUsage();
                    final long max = mu.getMax();
                    if (max < 0) {
                        continue;
                    }
                    final long alert = (max * Settings.settings().MAX_MEMORY_PERCENT) / 100;
                    mp.setUsageThreshold(alert);
                }
            }
        } catch (Throwable ignored) {
            LOGGER.error("FAWE encountered an error trying to listen to JVM memory.\n"
                    + "Please change your Java security settings or disable this message by"
                    + "changing 'max-memory-percent' in the config files to '-1'.");
        }
    }

    /**
     * Get the main thread.
     */
    public Thread getMainThread() {
        return this.thread;
    }

    /**
     * Sets the main thread to the current thread.
     */
    public Thread setMainThread() {
        return this.thread = Thread.currentThread();
    }

    /**
     * Gets the executor used for clipboard IO if clipboard on disk is enabled or null
     *
     * @return Executor used for clipboard IO if clipboard on disk is enabled or null
     * @since 2.6.2
     * @deprecated Use any of {@link Fawe#submitUUIDKeyQueuedTask(UUID, Runnable)},
     * {@link Fawe#submitUUIDKeyQueuedTask(UUID, Runnable, Object), {@link Fawe#submitUUIDKeyQueuedTask(UUID, Callable)}
     * to ensure if a thread is already a UUID-queued thread, the task is immediately run
     */
    @Deprecated(forRemoval = true, since = "TODO")
    public KeyQueuedExecutorService<UUID> getClipboardExecutor() {
        return this.uuidKeyQueuedExecutorService;
    }

    /**
     * Submit a task to the UUID key-queued executor
     *
     * @return Future representing the tank
     * @since TODO
     */
    public Future<?> submitUUIDKeyQueuedTask(UUID uuid, Runnable runnable) {
        if (Thread.currentThread() instanceof UUIDKeyQueuedThreadFactory.UUIDKeyQueuedThread) {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        }
        return this.uuidKeyQueuedExecutorService.submit(uuid, runnable);
    }

    /**
     * Submit a task to the UUID key-queued executor
     *
     * @return Future representing the tank
     * @since TODO
     */
    public <T> Future<T> submitUUIDKeyQueuedTask(UUID uuid, Runnable runnable, T result) {
        if (Thread.currentThread() instanceof UUIDKeyQueuedThreadFactory.UUIDKeyQueuedThread) {
            runnable.run();
            return CompletableFuture.completedFuture(result);
        }
        return this.uuidKeyQueuedExecutorService.submit(uuid, runnable, result);
    }

    /**
     * Submit a task to the UUID key-queued executor
     *
     * @return Future representing the tank
     * @since TODO
     */
    public <T> Future<T> submitUUIDKeyQueuedTask(UUID uuid, Callable<T> callable) {
        if (Thread.currentThread() instanceof UUIDKeyQueuedThreadFactory.UUIDKeyQueuedThread) {
            try {
                CompletableFuture.completedFuture(callable.call());
            } catch (Throwable t) {
                CompletableFuture.failedFuture(t);
            }
        }
        return this.uuidKeyQueuedExecutorService.submit(uuid, callable);
    }

}
