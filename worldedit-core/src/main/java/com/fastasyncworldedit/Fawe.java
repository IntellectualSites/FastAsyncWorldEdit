package com.fastasyncworldedit;

import com.fastasyncworldedit.beta.implementation.queue.QueueHandler;
import com.fastasyncworldedit.configuration.Settings;
import com.fastasyncworldedit.util.CachedTextureUtil;
import com.fastasyncworldedit.util.CleanTextureUtil;
import com.fastasyncworldedit.util.FaweTimer;
import com.fastasyncworldedit.util.MainUtil;
import com.fastasyncworldedit.util.MemUtil;
import com.fastasyncworldedit.util.RandomTextureUtil;
import com.fastasyncworldedit.util.TaskManager;
import com.fastasyncworldedit.util.TextureUtil;
import com.fastasyncworldedit.util.WEManager;
import com.github.luben.zstd.util.Native;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

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
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.NotificationEmitter;

/**
 * [ WorldEdit action]
 *  |
 * \|/
 * [ EditSession ] - The change is processed (area restrictions, change limit, block type)
 *  |
 * \|/
 * [Block change] - A block change from some location
 *  |
 * \|/
 * [ Set Queue ] - The SetQueue manages the implementation specific queue
 *  |
 * \|/
 * [ Fawe Queue] - A queue of chunks - check if the queue has the chunk for a change
 *  |
 * \|/
 * [ Fawe Chunk Implementation ] - Otherwise create a new FaweChunk object which is a wrapper around the Chunk object
 *  |
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
    private FaweVersion version;
    private TextureUtil textures;


    private QueueHandler queueHandler;

    /**
     * Get the implementation specific class.
     */
    @SuppressWarnings("unchecked")
    public static <T extends IFawe> T imp() {
        return instance != null ? (T) instance.implementation : null;
    }

    /**
     * Get the implementation independent class.
     */
    public static Fawe get() {
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

    /**
     * The platform specific implementation.
     */
    private final IFawe implementation;
    private Thread thread;

    private Fawe(final IFawe implementation) {
        instance = this;
        this.implementation = implementation;
        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        this.setupConfigs();
        TaskManager.IMP = this.implementation.getTaskManager();

        TaskManager.IMP.async(() -> {
            MainUtil.deleteOlder(MainUtil.getFile(this.implementation
                                                      .getDirectory(), Settings.IMP.PATHS.HISTORY), TimeUnit.DAYS.toMillis(Settings.IMP.HISTORY.DELETE_AFTER_DAYS), false);
            MainUtil.deleteOlder(MainUtil.getFile(this.implementation
                                                      .getDirectory(), Settings.IMP.PATHS.CLIPBOARD), TimeUnit.DAYS.toMillis(Settings.IMP.CLIPBOARD.DELETE_AFTER_DAYS), false);
        });

        /*
         * Instance independent stuff
         */
        this.setupMemoryListener();
        this.timer = new FaweTimer();

        // Delayed worldedit setup
        TaskManager.IMP.later(() -> {
            try {
                WEManager.IMP.managers.addAll(Fawe.this.implementation.getMaskManagers());
            } catch (Throwable ignored) {
            }
        }, 0);

        TaskManager.IMP.repeat(timer, 1);
    }

    public void onDisable() {
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
     * @apiNote Unofficial jars may be lacking version information
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
        Settings.IMP.PLATFORM = implementation.getPlatform().replace("\"", "");
        try (InputStream stream = getClass().getResourceAsStream("/fawe.properties");
            BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String versionString = br.readLine();
            String commitString = br.readLine();
            String dateString = br.readLine();
            br.close();
            this.version = FaweVersion.tryParse(versionString, commitString, dateString);
            Settings.IMP.DATE = new Date(100 + version.year, version.month, version.day).toString();
            Settings.IMP.BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit-1.16/" + version.build;
            Settings.IMP.COMMIT = "https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignored) {
        }
        try {
            Settings.IMP.reload(file);
        } catch (Throwable e) {
            LOGGER.error("Failed to load config.", e);
        }
    }


    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    public static void setupInjector() {
        /*
         * Modify the sessions
         *  - EditSession supports a custom queue, and a lot of optimizations
         *  - LocalSession supports VirtualPlayers and undo on disk
         */
        if (!Settings.IMP.EXPERIMENTAL.DISABLE_NATIVES) {
            try {
                Native.load();
            } catch (Throwable e) {
                if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL > 6 || Settings.IMP.HISTORY.COMPRESSION_LEVEL > 6) {
                    Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
                    Settings.IMP.HISTORY.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.HISTORY.COMPRESSION_LEVEL);
                    LOGGER.error("ZSTD Compression Binding Not Found.\n"
                                  + "FAWE will still work but compression won't work as well.\n", e);
                }
            }
            try {
                net.jpountz.util.Native.load();
            } catch (Throwable e) {
                LOGGER.error("LZ4 Compression Binding Not Found.\n"
                              + "FAWE will still work but compression will be slower.\n", e);
            }
        }

        // Check Base OS Arch for Mismatching Architectures
        boolean x86OS = System.getProperty("sun.arch.data.model").contains("32");
        boolean x86JVM = System.getProperty("os.arch").contains("32");
        if (x86OS != x86JVM) {
            LOGGER.info("You are running 32-bit Java on a 64-bit machine. Please upgrade to 64-bit Java.");
        }
    }

    private void setupMemoryListener() {
        if (Settings.IMP.MAX_MEMORY_PERCENT < 1 || Settings.IMP.MAX_MEMORY_PERCENT > 99) {
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
                    final long alert = (max * Settings.IMP.MAX_MEMORY_PERCENT) / 100;
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

    public static boolean isMainThread() {
        return instance == null || instance.thread == Thread.currentThread();
    }

    /**
     * Sets the main thread to the current thread.
     */
    public Thread setMainThread() {
        return this.thread = Thread.currentThread();
    }
}
