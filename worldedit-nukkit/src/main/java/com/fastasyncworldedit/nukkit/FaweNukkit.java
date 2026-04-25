package com.fastasyncworldedit.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.IFawe;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.preloader.AsyncPreloader;
import com.fastasyncworldedit.core.queue.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.TaskManager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Nukkit implementation of the FAWE platform contract ({@link IFawe}).
 * <p>
 * This class wires FAWE's core async engine to Nukkit's scheduler and
 * chunk system. Compared to Bukkit, the Nukkit implementation is simpler:
 * there is no region protection plugin integration (getMaskManagers returns
 * an empty list), and the queue handler operates with fewer platform-specific
 * optimizations because Nukkit's API surface is smaller.
 * <p>
 * Preloading uses the standard AsyncPreloader since Nukkit does not provide
 * specialized chunk loading hooks. The platform adapter delegates chunk
 * sending to Nukkit's internal mechanisms, which means some FAWE features
 * like fake chunk packets are unavailable.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>No FaweMaskManager support; no region plugin integration</li>
 *   <li>Simpler queue handler with fewer NMS optimizations</li>
 *   <li>Chunk packet sending is internal to Nukkit</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.core.IFawe
 * @see com.fastasyncworldedit.bukkit.FaweBukkit
 */
public class FaweNukkit implements IFawe {

    private final Plugin plugin;
    private final NukkitTaskManager taskManager;
    private final NukkitPlatformAdapter platformAdapter;
    private NukkitQueueHandler queueHandler;
    private Preloader preloader;

    public FaweNukkit(Plugin plugin) {
        this.plugin = plugin;
        this.taskManager = new NukkitTaskManager(plugin);
        this.platformAdapter = new NukkitPlatformAdapter();
        try {
            Fawe.set(this);
            Fawe.setupInjector();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize FAWE", e);
        }
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return Collections.emptyList();
    }

    @Override
    public String getPlatform() {
        return "Nukkit";
    }

    @Override
    public UUID getUUID(String name) {
        Player player = Server.getInstance().getPlayerExact(name);
        if (player != null) {
            return player.getUniqueId();
        }
        return null;
    }

    @Override
    public String getName(UUID uuid) {
        Server server = Server.getInstance();
        for (Player player : server.getOnlinePlayers().values()) {
            if (player.getUniqueId().equals(uuid)) {
                return player.getName();
            }
        }
        return null;
    }

    @Override
    public QueueHandler getQueueHandler() {
        if (queueHandler == null) {
            synchronized (this) {
                if (queueHandler == null) {
                    queueHandler = new NukkitQueueHandler();
                }
            }
        }
        return queueHandler;
    }

    @Override
    public Preloader getPreloader(boolean initialise) {
        if (preloader == null && initialise) {
            preloader = new AsyncPreloader();
        }
        return preloader;
    }

    @Override
    public FAWEPlatformAdapterImpl getPlatformAdapter() {
        return platformAdapter;
    }

    public Plugin getPlugin() {
        return plugin;
    }

}
