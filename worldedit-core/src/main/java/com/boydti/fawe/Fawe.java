package com.boydti.fawe;

import com.boydti.fawe.command.Cancel;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.VisualQueue;
import com.boydti.fawe.regions.general.plot.PlotSquaredFeature;
import com.boydti.fawe.util.*;
import com.boydti.fawe.util.chat.ChatManager;
import com.boydti.fawe.util.chat.PlainChatManager;
import com.boydti.fawe.util.cui.CUI;
import com.boydti.fawe.util.metrics.BStats;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.session.request.Request;

import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

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
 * \ e.g. A blob of stone: only the visible blocks need to have the lighting calculated
 * - Block changes are sent with a chunk packet
 * \ A chunk packet is generally quicker to create and smaller for large world edits
 * - No physics updates
 * \ Physics updates are slow, and are usually performed on each block
 * - Block data shortcuts
 * \ Some known blocks don't need to have the data set or accessed (e.g. air is never going to have data)
 * - Remove redundant extents
 * \ Up to 11 layers of extents can be removed
 * - History bypassing
 * \ FastMode bypasses history and means blocks in the world don't need to be checked and recorded
 */
public class Fawe {
    /**
     * The FAWE instance;
     */
    private static Fawe INSTANCE;

    /**
     * TPS timer
     */
    private final FaweTimer timer;
    private FaweVersion version;
    private VisualQueue visualQueue;
    private Updater updater;
    private TextureUtil textures;
    private DefaultTransformParser transformParser;
    private ChatManager chatManager = new PlainChatManager();

    private BStats stats;

    /**
     * Get the implementation specific class
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends IFawe> T imp() {
        return INSTANCE != null ? (T) INSTANCE.IMP : null;
    }

    /**
     * Get the implementation independent class
     *
     * @return
     */
    public static Fawe get() {
        return INSTANCE;
    }

    /**
     * Setup Fawe
     *
     * @param implementation
     * @throws InstanceAlreadyExistsException
     */
    public static void set(final IFawe implementation) throws InstanceAlreadyExistsException, IllegalArgumentException {
        if (INSTANCE != null) {
            throw new InstanceAlreadyExistsException("FAWE has already been initialized with: " + INSTANCE.IMP);
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation may not be null.");
        }
        INSTANCE = new Fawe(implementation);
    }

    public static void debugPlain(String s) {
        if (INSTANCE != null) {
            INSTANCE.IMP.debug(s);
        } else {
            System.out.println(BBC.stripColor(BBC.color(s)));
        }
    }

    /**
     * Write something to the console
     *
     * @param s
     */
    public static void debug(Object s) {
        Actor actor = Request.request().getActor();
        if (actor != null && actor.isPlayer()) {
            actor.print(BBC.color(BBC.PREFIX.original() + " " + s));
            return;
        }
        debugPlain(BBC.PREFIX.original() + " " + s);
    }

    /**
     * The platform specific implementation
     */
    private final IFawe IMP;
    private Thread thread = Thread.currentThread();

    private Fawe(final IFawe implementation) {
        this.INSTANCE = this;
        this.IMP = implementation;
        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        this.setupConfigs();
        TaskManager.IMP = this.IMP.getTaskManager();

        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.IMP.PATHS.HISTORY), TimeUnit.DAYS.toMillis(Settings.IMP.HISTORY.DELETE_AFTER_DAYS), false);
                MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.IMP.PATHS.CLIPBOARD), TimeUnit.DAYS.toMillis(Settings.IMP.CLIPBOARD.DELETE_AFTER_DAYS), false);
            }
        });

        if (Settings.IMP.METRICS) {
            try {
                this.stats = new BStats();
                this.IMP.startMetrics();
                TaskManager.IMP.later(new Runnable() {
                    @Override
                    public void run() {
                        stats.start();
                    }
                }, 1);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
        this.setupCommands();
        /*
         * Instance independent stuff
         */
        this.setupMemoryListener();
        this.timer = new FaweTimer();
        Fawe.this.IMP.setupVault();

        File jar = MainUtil.getJarFile();
        // TODO FIXME remove extrablocks.json
//        File extraBlocks = MainUtil.copyFile(jar, "extrablocks.json", null);
//        if (extraBlocks != null && extraBlocks.exists()) {
//            TaskManager.IMP.task(() -> {
//                try {
//                    BundledBlockData.getInstance().loadFromResource();
//                    BundledBlockData.getInstance().add(extraBlocks.toURI().toURL(), true);
//                } catch (Throwable ignore) {
//                    ignore.printStackTrace();
//                    Fawe.debug("Invalid format: extrablocks.json");
//                }
//            });
//        }

        // Delayed worldedit setup
        TaskManager.IMP.later(() -> {
            try {
                transformParser = new DefaultTransformParser(getWorldEdit());
                visualQueue = new VisualQueue(3);
                WEManager.IMP.managers.addAll(Fawe.this.IMP.getMaskManagers());
                WEManager.IMP.managers.add(new PlotSquaredFeature());
                Fawe.debug("Plugin 'PlotSquared' found. Using it now.");
            } catch (Throwable e) {}
        }, 0);

        TaskManager.IMP.repeat(timer, 1);

        if (!Settings.IMP.UPDATE.equalsIgnoreCase("false")) {
            // Delayed updating
            updater = new Updater();
            TaskManager.IMP.async(() -> update());
            TaskManager.IMP.repeatAsync(() -> update(), 36000);
        }
    }

    public void onDisable() {
        if (stats != null) {
            stats.close();
        }
    }

    private boolean update() {
        if (updater != null) {
            updater.getUpdate(IMP.getPlatform(), getVersion());
            return true;
        }
        return false;
    }

    public CUI getCUI(Actor actor) {
        FawePlayer<Object> fp = FawePlayer.wrap(actor);
        CUI cui = fp.getMeta("CUI");
        if (cui == null) {
            cui = Fawe.imp().getCUI(fp);
            if (cui != null) {
                synchronized (fp) {
                    CUI tmp = fp.getMeta("CUI");
                    if (tmp == null) {
                        fp.setMeta("CUI", cui);
                    } else {
                        cui = tmp;
                    }
                }
            }
        }
        return cui;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void setChatManager(ChatManager chatManager) {
        checkNotNull(chatManager);
        this.chatManager = chatManager;
    }

    //    @Deprecated
//    public boolean isJava8() {
//        return isJava8;
//    }

    public DefaultTransformParser getTransformParser() {
        return transformParser;
    }

    /**
     * The FAWE updater class
     * - Use to get basic update information (changelog/version etc)
     *
     * @return
     */
    public Updater getUpdater() {
        return updater;
    }

    public TextureUtil getCachedTextureUtil(boolean randomize, int min, int max) {
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
     * The FaweTimer is a useful class for monitoring TPS
     *
     * @return FaweTimer
     */
    public FaweTimer getTimer() {
        return timer;
    }

    /**
     * The visual queue is used to queue visualizations
     *
     * @return
     */
    public VisualQueue getVisualQueue() {
        return visualQueue;
    }

    /**
     * The FAWE version
     * - Unofficial jars may be lacking version information
     *
     * @return FaweVersion
     */
    public
    @Nullable
    FaweVersion getVersion() {
        return version;
    }

    public double getTPS() {
        return timer.getTPS();
    }

    private void setupCommands() {
        this.IMP.setupCommand("fcancel", new Cancel());
    }

    public void setupConfigs() {
        MainUtil.copyFile(MainUtil.getJarFile(), "de/message.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "ru/message.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "ru/commands.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "tr/message.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "es/message.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "es/commands.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "nl/message.yml", null);
        MainUtil.copyFile(MainUtil.getJarFile(), "fr/message.yml", null);
        // Setting up config.yml
        File file = new File(this.IMP.getDirectory(), "config.yml");
        Settings.IMP.PLATFORM = IMP.getPlatform().replace("\"", "");
        try {
            InputStream stream = getClass().getResourceAsStream("/fawe.properties");
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            String versionString = scanner.next().trim();
            scanner.close();
            this.version = new FaweVersion(versionString);
            Settings.IMP.DATE = new Date(100 + version.year, version.month, version.day).toGMTString();
            Settings.IMP.BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
            Settings.IMP.COMMIT = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignore) {}
        try {
            Settings.IMP.reload(file);
            // Setting up message.yml
            String lang = Objects.toString(Settings.IMP.LANGUAGE);
            BBC.load(new File(this.IMP.getDirectory(), (lang.isEmpty() ? "" : lang + File.separator) + "message.yml"));
            Commands.load(new File(INSTANCE.IMP.getDirectory(), "commands.yml"));
        } catch (Throwable e) {
            debug("====== Failed to load config ======");
            debug("Please validate your yaml files:");
            debug("====================================");
            e.printStackTrace();
            debug("====================================");
        }
    }


    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    public static void setupInjector() {
        /*
         * Modify the sessions
         *  - EditSession supports custom queue and a lot of optimizations
         *  - LocalSession supports VirtualPlayers and undo on disk
         */
        if (!Settings.IMP.EXPERIMENTAL.DISABLE_NATIVES) {
            try {
                com.github.luben.zstd.util.Native.load();
            } catch (Throwable e) {
                if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL > 6 || Settings.IMP.HISTORY.COMPRESSION_LEVEL > 6) {
                    Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
                    Settings.IMP.HISTORY.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.HISTORY.COMPRESSION_LEVEL);
                    debug("====== ZSTD COMPRESSION BINDING NOT FOUND ======");
                    debug(e);
                    debug("===============================================");
                    debug("FAWE will work but won't compress data as much");
                    debug("===============================================");
                }
            }
            try {
                net.jpountz.util.Native.load();
            } catch (Throwable e) {
                e.printStackTrace();
                debug("====== LZ4 COMPRESSION BINDING NOT FOUND ======");
                debug(e);
                debug("===============================================");
                debug("FAWE will work but compression will be slower");
                debug(" - Try updating your JVM / OS");
                debug(" - Report this issue if you cannot resolve it");
                debug("===============================================");
            }
        }
        try {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            boolean x86OS = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? false : true;
            boolean x86JVM = System.getProperty("sun.arch.data.model").equals("32");
            if (x86OS != x86JVM) {
                debug("====== UPGRADE TO 64-BIT JAVA ======");
                debug("You are running 32-bit Java on a 64-bit machine");
                debug(" - This is only a recommendation");
                debug("====================================");
            }
        } catch (Throwable ignore) {}
    }

    private void setupMemoryListener() {
        if (Settings.IMP.MAX_MEMORY_PERCENT < 1 || Settings.IMP.MAX_MEMORY_PERCENT > 99) {
            return;
        }
        try {
            final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            final NotificationEmitter ne = (NotificationEmitter) memBean;

            ne.addNotificationListener(new NotificationListener() {
                @Override
                public void handleNotification(final Notification notification, final Object handback) {
                    final long heapSize = Runtime.getRuntime().totalMemory();
                    final long heapMaxSize = Runtime.getRuntime().maxMemory();
                    if (heapSize < heapMaxSize) {
                        return;
                    }
                    MemUtil.memoryLimitedTask();
                }
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
        } catch (Throwable e) {
            debug("====== MEMORY LISTENER ERROR ======");
            MainUtil.handleError(e, false);
            debug("===================================");
            debug("FAWE needs access to the JVM memory system:");
            debug(" - Change your Java security settings");
            debug(" - Disable this with `max-memory-percent: -1`");
            debug("===================================");
        }
    }

    /**
     * Get the main thread
     *
     * @return
     */
    public Thread getMainThread() {
        return this.thread;
    }

    public static boolean isMainThread() {
        return INSTANCE != null ? INSTANCE.thread == Thread.currentThread() : true;
    }

    /**
     * Sets the main thread to the current thread
     *
     * @return
     */
    public Thread setMainThread() {
        return this.thread = Thread.currentThread();
    }

    private ConcurrentHashMap<String, FawePlayer> players = new ConcurrentHashMap<>(8, 0.9f, 1);
    private ConcurrentHashMap<UUID, FawePlayer> playersUUID = new ConcurrentHashMap<>(8, 0.9f, 1);

    public <T> void register(FawePlayer<T> player) {
        players.put(player.getName(), player);
        playersUUID.put(player.getUUID(), player);

    }

    public <T> void unregister(String name) {
        FawePlayer player = players.remove(name);
        if (player != null) playersUUID.remove(player.getUUID());
    }

    public FawePlayer getCachedPlayer(String name) {
        return players.get(name);
    }

    public FawePlayer getCachedPlayer(UUID uuid) {
        return playersUUID.get(uuid);
    }

    public Collection<FawePlayer> getCachedPlayers() {
        return players.values();
    }
}
