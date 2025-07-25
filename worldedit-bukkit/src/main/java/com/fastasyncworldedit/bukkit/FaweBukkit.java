package com.fastasyncworldedit.bukkit;

import com.fastasyncworldedit.bukkit.adapter.BukkitQueueHandler;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.bukkit.listener.BrushListener;
import com.fastasyncworldedit.bukkit.listener.ChunkListener9;
import com.fastasyncworldedit.bukkit.listener.RenderListener;
import com.fastasyncworldedit.bukkit.regions.GriefDefenderFeature;
import com.fastasyncworldedit.bukkit.regions.GriefPreventionFeature;
import com.fastasyncworldedit.bukkit.regions.ResidenceFeature;
import com.fastasyncworldedit.bukkit.regions.TownyFeature;
import com.fastasyncworldedit.bukkit.regions.WorldGuardFeature;
import com.fastasyncworldedit.bukkit.util.BukkitTaskManager;
import com.fastasyncworldedit.bukkit.util.ItemUtil;
import com.fastasyncworldedit.bukkit.util.image.BukkitImageViewer;
import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.IFawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.preloader.AsyncPreloader;
import com.fastasyncworldedit.core.queue.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.WEManager;
import com.fastasyncworldedit.core.util.image.ImageViewer;
import com.plotsquared.core.PlotSquared;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import io.papermc.lib.PaperLib;
import io.papermc.paper.datapack.Datapack;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class FaweBukkit implements IFawe, Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final Plugin plugin;
    private final FAWEPlatformAdapterImpl platformAdapter;
    private ItemUtil itemUtil;
    private Preloader preloader;
    private volatile boolean keepUnloaded;

    public FaweBukkit(Plugin plugin) {
        this.plugin = plugin;
        try {
            Fawe.set(this);
            Fawe.setupInjector();
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                LOGGER.error("Brush Listener Failed", e);
            }
            if (PaperLib.isPaper() && Settings.settings().EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        platformAdapter = new NMSAdapter();

        //PlotSquared support is limited to Spigot/Paper as of 02/20/2020
        TaskManager.taskManager().later(this::setupPlotSquared, 0);

        // Registered delayed Event Listeners
        TaskManager.taskManager().task(() -> {
            // Fix for ProtocolSupport
            Settings.settings().PROTOCOL_SUPPORT_FIX =
                    Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");

            // This class
            Bukkit.getPluginManager().registerEvents(FaweBukkit.this, FaweBukkit.this.plugin);

            // The tick limiter
            new ChunkListener9();
        });

        // Warn if small-edits are enabled with extended world heights
        if (Settings.settings().HISTORY.SMALL_EDITS) {
            LOGGER.warn("Small-edits enabled (maximum y range of 0 -> 256) with 1.18+ world heights. Are you sure?");
        }
    }

    @Override
    public QueueHandler getQueueHandler() {
        return new BukkitQueueHandler();
    }

    @Override
    public synchronized ImageViewer getImageViewer(com.sk89q.worldedit.entity.Player player) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    public ItemUtil getItemUtil() {
        ItemUtil tmp = itemUtil;
        if (tmp == null) {
            try {
                this.itemUtil = tmp = new ItemUtil();
            } catch (Throwable e) {
                Settings.settings().EXPERIMENTAL.PERSISTENT_BRUSHES = false;
                LOGGER.error("Persistent Brushes Failed", e);
            }
        }
        return tmp;
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();

        msg.append("# FastAsyncWorldEdit Information\n");
        msg.append(Fawe.instance().getVersion()).append("\n\n");

        List<Plugin> plugins = new ArrayList<>();
        Collections.addAll(plugins, Bukkit.getServer().getPluginManager().getPlugins());
        plugins.sort(Comparator.comparing(Plugin::getName));

        msg.append("Server Version: ").append(Bukkit.getVersion()).append("\n");
        msg.append("Plugins (").append(plugins.size()).append("):\n");
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
        int dataVersion = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getDataVersion();
        if (dataVersion >= 2586 && PaperLib.isPaper()) {
            Collection<Datapack> datapacks = Bukkit.getServer().getDatapackManager().getEnabledPacks();
            msg.append("Enabled Datapacks (").append(datapacks.size()).append("):\n");
            for (Datapack dp : datapacks) {
                msg.append(" - ").append(dp.getName()).append("\n");
            }
        }
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks.
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskManager(plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * A mask manager handles region restrictions e.g., PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin =
                Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if (worldguardPlugin != null && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new WorldGuardFeature(worldguardPlugin));
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
                managers.add(new ResidenceFeature(residencePlugin));
                LOGGER.info("Attempting to use plugin 'Residence'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefpreventionPlugin =
                Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                LOGGER.info("Attempting to use plugin 'GriefPrevention'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefdefenderPlugin =
                Bukkit.getServer().getPluginManager().getPlugin("GriefDefender");
        if (griefdefenderPlugin != null && griefdefenderPlugin.isEnabled()) {
            try {
                managers.add(new GriefDefenderFeature(griefdefenderPlugin));
                LOGGER.info("Attempting to use plugin 'GriefDefender'");
            } catch (Throwable ignored) {
            }
        }

        return managers;
    }

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

    @Override
    public String getPlatform() {
        return "Bukkit";
    }

    @Override
    public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public Preloader getPreloader(boolean initialise) {
        if (PaperLib.isPaper()) {
            if (preloader == null && initialise) {
                return preloader = new AsyncPreloader();
            }
            return preloader;
        }
        return null;
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
        if (PlotSquared.get().getVersion().version[0] == 7) {
            WEManager.weManager().addManager(new com.fastasyncworldedit.bukkit.regions.plotsquared.PlotSquaredFeature());
            LOGGER.info("Plugin 'PlotSquared' v7 found. Using it now.");
        } else {
            LOGGER.error("Incompatible version of PlotSquared found. Please use PlotSquared v7.");
            LOGGER.info("https://www.spigotmc.org/resources/77506/");
        }
    }

}
