package com.fastasyncworldedit.bukkit;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.IFawe;
import com.fastasyncworldedit.core.beta.implementation.preloader.AsyncPreloader;
import com.fastasyncworldedit.core.beta.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.beta.implementation.queue.QueueHandler;
import com.fastasyncworldedit.bukkit.adapter.BukkitQueueHandler;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.bukkit.listener.BrushListener;
import com.fastasyncworldedit.bukkit.listener.ChunkListener9;
import com.fastasyncworldedit.bukkit.listener.RenderListener;
import com.fastasyncworldedit.bukkit.regions.GriefPreventionFeature;
import com.fastasyncworldedit.bukkit.regions.ResidenceFeature;
import com.fastasyncworldedit.bukkit.regions.TownyFeature;
import com.fastasyncworldedit.bukkit.regions.Worldguard;
import com.fastasyncworldedit.bukkit.util.BukkitTaskManager;
import com.fastasyncworldedit.bukkit.util.ItemUtil;
import com.fastasyncworldedit.bukkit.util.MinecraftVersion;
import com.fastasyncworldedit.bukkit.util.image.BukkitImageViewer;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.ThirdPartyManager;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.WEManager;
import com.fastasyncworldedit.core.util.image.ImageViewer;
import com.plotsquared.core.PlotSquared;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import io.papermc.lib.PaperLib;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

public class FaweBukkit implements IFawe, Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final Plugin plugin;
    private ItemUtil itemUtil;

    private boolean listeningImages;
    private final boolean chunksStretched;
    private final FAWEPlatformAdapterImpl platformAdapter;

    public FaweBukkit(Plugin plugin) {
        this.plugin = plugin;
        try {
            Settings.IMP.TICK_LIMITER.ENABLED = !Bukkit.hasWhitelist();
            Fawe.set(this);
            Fawe.setupInjector();
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                LOGGER.error("Brush Listener Failed", e);
            }
            if (PaperLib.isPaper() && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        chunksStretched = new MinecraftVersion().isEqualOrHigher(MinecraftVersion.NETHER);

        platformAdapter = new NMSAdapter();

        //PlotSquared support is limited to Spigot/Paper as of 02/20/2020
        TaskManager.IMP.later(this::setupPlotSquared, 0);

        // Registered delayed Event Listeners
        TaskManager.IMP.task(() -> {
            // Fix for ProtocolSupport
            Settings.IMP.PROTOCOL_SUPPORT_FIX =
                Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");

            // This class
            Bukkit.getPluginManager().registerEvents(FaweBukkit.this, FaweBukkit.this.plugin);

            // The tick limiter
            new ChunkListener9();
        });
    }

    @Override public QueueHandler getQueueHandler() {
        return new BukkitQueueHandler();
    }

    @Override
    public synchronized ImageViewer getImageViewer(com.sk89q.worldedit.entity.Player player) {
        try {
            listeningImages = true;
            PluginManager manager = Bukkit.getPluginManager();

            if (manager.getPlugin("PacketListenerApi") == null) {
                File output = new File(plugin.getDataFolder().getParentFile(),
                    "PacketListenerAPI_v3.7.6-SNAPSHOT.jar");
                byte[] jarData = ThirdPartyManager.PacketListenerAPI.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            if (manager.getPlugin("MapManager") == null) {
                File output = new File(plugin.getDataFolder().getParentFile(),
                    "MapManager_v1.7.8-SNAPSHOT.jar");
                byte[] jarData = ThirdPartyManager.MapManager.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            return new BukkitImageViewer(BukkitAdapter.adapt(player));
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override public File getDirectory() {
        return plugin.getDataFolder();
    }


    public ItemUtil getItemUtil() {
        ItemUtil tmp = itemUtil;
        if (tmp == null) {
            try {
                this.itemUtil = tmp = new ItemUtil();
            } catch (Throwable e) {
                Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES = false;
                LOGGER.error("Persistent Brushes Failed", e);
            }
        }
        return tmp;
    }

    @Override public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();
        Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
        msg.append("Server Version: ").append(Bukkit.getVersion()).append("\n");
        msg.append("Plugins (").append(plugins.length).append("): \n");
        for (Plugin p : plugins) {
            msg.append(" - ").append(p.getName()).append(":").append("\n")
                .append("  • Version: ").append(p.getDescription().getVersion()).append("\n")
                .append("  • Enabled: ").append(p.isEnabled()).append("\n")
                .append("  • Main: ").append(p.getDescription().getMain()).append("\n")
                .append("  • Authors: ").append(p.getDescription().getAuthors()).append("\n")
                .append("  • Load Before: ").append(p.getDescription().getLoadBefore()).append("\n")
                .append("  • Dependencies: ").append(p.getDescription().getDepend()).append("\n")
                .append("  • Soft Dependencies: ").append(p.getDescription().getSoftDepend()).append("\n")
                .append("  • Provides: ").append(p.getDescription().getProvides()).append("\n");
        }
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks.
     */
    @Override public TaskManager getTaskManager() {
        return new BukkitTaskManager(plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * A mask manager handles region restrictions e.g., PlotSquared plots / WorldGuard regions
     */
    @Override public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin =
            Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if (worldguardPlugin != null && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new Worldguard(worldguardPlugin));
                LOGGER.info("Attempting to use plugin 'WorldGuard'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin));
                LOGGER.info("Attempting to use plugin 'Towny'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if (residencePlugin != null && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                LOGGER.info("Attempting to use plugin 'Residence'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefpreventionPlugin =
            Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                LOGGER.debug("Attempting to use plugin 'GriefPrevention'");
            } catch (Throwable ignored) {
            }
        }

        return managers;
    }

    private volatile boolean keepUnloaded;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (keepUnloaded) {
            org.bukkit.World world = event.getWorld();
            world.setKeepSpawnInMemory(false);
        }
    }

    public synchronized <T> T createWorldUnloaded(Supplier<T> task) {
        keepUnloaded = true;
        try {
            return task.get();
        } finally {
            keepUnloaded = false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
        wePlayer.unregister();
    }

    @Override public String getPlatform() {
        return "Bukkit";
    }

    @Override public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override public Preloader getPreloader() {
        if (PaperLib.isPaper()) {
            return new AsyncPreloader();
        }
        return null;
    }

    @Override public boolean isChunksStretched() {
        return chunksStretched;
    }

    @Override
    public FAWEPlatformAdapterImpl getPlatformAdapter() {
        return platformAdapter;
    }

    private void setupPlotSquared() {
        Plugin plotSquared = this.plugin.getServer().getPluginManager().getPlugin("PlotSquared");
        if (plotSquared == null) {
            return;
        }
        if (plotSquared.getClass().getPackage().toString().contains("intellectualsites")) {
            WEManager.IMP.managers.add(new com.fastasyncworldedit.bukkit.regions.plotsquaredv4.PlotSquaredFeature());
            LOGGER.info("Plugin 'PlotSquared' found. Using it now.");
        } else if (PlotSquared.get().getVersion().version[0] == 6){
            WEManager.IMP.managers.add(new com.fastasyncworldedit.bukkit.regions.plotsquared.PlotSquaredFeature());
            LOGGER.info("Plugin 'PlotSquared' found. Using it now.");
        } else {
            LOGGER.error("Incompatible version of PlotSquared found. Please use PlotSquared v6.");
        }
    }
}
