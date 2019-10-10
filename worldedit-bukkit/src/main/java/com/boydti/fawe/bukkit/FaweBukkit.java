package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.bukkit.adapter.BukkitQueueHandler;
import com.boydti.fawe.bukkit.listener.BrushListener;
import com.boydti.fawe.bukkit.listener.BukkitImageListener;
import com.boydti.fawe.bukkit.listener.ChunkListener_8;
import com.boydti.fawe.bukkit.listener.ChunkListener_9;
import com.boydti.fawe.bukkit.listener.RenderListener;
import com.boydti.fawe.bukkit.regions.ASkyBlockHook;
import com.boydti.fawe.bukkit.regions.FactionsFeature;
import com.boydti.fawe.bukkit.regions.FactionsUUIDFeature;
import com.boydti.fawe.bukkit.regions.FreeBuildRegion;
import com.boydti.fawe.bukkit.regions.GriefPreventionFeature;
import com.boydti.fawe.bukkit.regions.ResidenceFeature;
import com.boydti.fawe.bukkit.regions.TownyFeature;
import com.boydti.fawe.bukkit.regions.Worldguard;
import com.boydti.fawe.bukkit.regions.WorldguardFlag;
import com.boydti.fawe.bukkit.regions.plotquared.PlotSquaredFeature;
import com.boydti.fawe.bukkit.util.BukkitTaskMan;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.bukkit.util.VaultUtil;
import com.boydti.fawe.bukkit.util.image.BukkitImageViewer;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.Jars;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FaweBukkit implements IFawe, Listener {

//    private final WorldEditPlugin plugin;
    private final Plugin plugin;
    private VaultUtil vault;
    private ItemUtil itemUtil;

    private boolean listeningImages;
    private BukkitImageListener imageListener;
    //private CFIPacketListener packetListener;

    public VaultUtil getVault() {
        return this.vault;
    }

    public FaweBukkit(Plugin plugin) {
        this.plugin = plugin;
        try {
            Settings.IMP.TICK_LIMITER.ENABLED = !Bukkit.hasWhitelist();
            Fawe.set(this);
            Fawe.setupInjector();
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                debug("====== BRUSH LISTENER FAILED ======");
                e.printStackTrace();
                debug("===================================");
            }
            if (PaperLib.isPaper() && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
            }
            if (Bukkit.getPluginManager().getPlugin("PlotSquared") != null) {
                try {
                    WEManager.IMP.managers.add(new PlotSquaredFeature());
                } catch (Exception ignored) {
                    //Not everyone uses or needs PlotSquared.
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        // Registered delayed Event Listeners
        TaskManager.IMP.task(() -> {
            // Fix for ProtocolSupport
            Settings.IMP.PROTOCOL_SUPPORT_FIX = Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");

            // This class
            Bukkit.getPluginManager().registerEvents(FaweBukkit.this, FaweBukkit.this.plugin);

            // The tick limiter
            try {
                Class.forName("sun.misc.SharedSecrets");
                new ChunkListener_8();
            } catch (ClassNotFoundException e) {
                new ChunkListener_9();
            }

        });
    }

//    @Override // Please don't delete this again, it's WIP
//    public void registerPacketListener() {
//        PluginManager manager = Bukkit.getPluginManager();
//        if (packetListener == null && manager.getPlugin("ProtocolLib") != null) {
//            packetListener = new CFIPacketListener(plugin);
//        }
//    }

    @Override
    public QueueHandler getQueueHandler() {
        return new BukkitQueueHandler();
    }

    @Override
    public synchronized ImageViewer getImageViewer(com.sk89q.worldedit.entity.Player fp) {
        if (listeningImages && imageListener == null) return null;
        try {
            listeningImages = true;
            //registerPacketListener();
            PluginManager manager = Bukkit.getPluginManager();

            if (manager.getPlugin("PacketListenerApi") == null) {
                File output = new File(plugin.getDataFolder().getParentFile(), "PacketListenerAPI_v3.6.0-SNAPSHOT.jar");
                byte[] jarData = Jars.PL_v3_6_0.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            if (manager.getPlugin("MapManager") == null) {
                File output = new File(plugin.getDataFolder().getParentFile(), "MapManager_v1.4.0-SNAPSHOT.jar");
                byte[] jarData = Jars.MM_v1_4_0.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            BukkitImageViewer viewer = new BukkitImageViewer(BukkitAdapter.adapt(fp));
            if (imageListener == null) {
                this.imageListener = new BukkitImageListener(plugin);
            }
            return viewer;
        } catch (Throwable ignore) {}
        return null;
    }

    @Override
    public int getPlayerCount() {
        return plugin.getServer().getOnlinePlayers().size();
    }

    @Override
    public boolean isOnlineMode() {
        return Bukkit.getOnlineMode();
    }

    @Override
    public String getPlatformVersion() {
        String bukkitVersion = Bukkit.getVersion();
        int index = bukkitVersion.indexOf("MC: ");
        return index == -1 ? bukkitVersion : bukkitVersion.substring(index + 4, bukkitVersion.length() - 1);
    }

    @Override
    public void debug(final String message) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(message);
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void setupCommand(final String label, final FaweCommand cmd) {
        if (plugin instanceof JavaPlugin) {
            TaskManager.IMP.task(() -> {
                PluginCommand registered = ((JavaPlugin) plugin).getCommand(label);
                if (registered == null) {
                    debug("Command not registered in plugin.yml: " + label);
                    return;
                }
                registered.setExecutor(new BukkitCommand(cmd));
            });
        }
    }

    @Override
    public com.sk89q.worldedit.entity.Player wrap(final Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            com.sk89q.worldedit.entity.Player existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            Player player = Bukkit.getPlayer(name);
            return player != null ? BukkitAdapter.adapt(player) : null;
        }
        if (obj.getClass() == UUID.class) {
            UUID uuid = (UUID) obj;
            com.sk89q.worldedit.entity.Player existing = Fawe.get().getCachedPlayer(uuid);
            if (existing != null) {
                return existing;
            }
            Player player = Bukkit.getPlayer(uuid);
            return player != null ? BukkitAdapter.adapt(player) : null;
        }
        return null;
    }

    @Override public void startMetrics() {
        new Metrics(plugin);
    }

    public ItemUtil getItemUtil() {
        ItemUtil tmp = itemUtil;
        if (tmp == null) {
            try {
                this.itemUtil = tmp = new ItemUtil();
            } catch (Throwable e) {
                Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES = false;
                debug("===== PERSISTENT BRUSH FAILED =====");
                e.printStackTrace();
                debug("===================================");
            }
        }
        return tmp;
    }

    /**
     * Vault isn't required, but used for setting player permissions (WorldEdit bypass)
     * @return
     */
    @Override
    public void setupVault() {
        try {
            this.vault = new VaultUtil();
        } catch (final Throwable e) {
            this.debug("&dVault is used for persistent `/wea` toggles.");
        }
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();
        msg.append("server.version: " + Bukkit.getVersion() + "\n");
        msg.append("Plugins: \n");
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            msg.append(" - " + p.getName() + ": " + p.getDescription().getVersion() + "\n");
        }
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    /**
     * A mask manager handles region restrictions e.g. PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if (worldguardPlugin != null && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new Worldguard(worldguardPlugin));
                managers.add(new WorldguardFlag(worldguardPlugin));
                Fawe.debug("Attempting to use plugin 'WorldGuard'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin));
                Fawe.debug("Attempting to use plugin 'Towny'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin factionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
        if (factionsPlugin != null && factionsPlugin.isEnabled()) {
            try {
                managers.add(new FactionsFeature(factionsPlugin));
                Fawe.debug("Attempting to use plugin 'Factions'");
            } catch (Throwable e) {
                try {
                    managers.add(new FactionsUUIDFeature(factionsPlugin, this));
                    Fawe.debug("Attempting to use plugin 'FactionsUUID'");
                } catch (Throwable ignored) {
                }
            }
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if (residencePlugin != null && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                Fawe.debug("Attempting to use plugin 'Residence'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                Fawe.debug("Attempting to use plugin 'GriefPrevention'");
            } catch (Throwable ignored) {
            }
        }

        final Plugin aSkyBlock = Bukkit.getServer().getPluginManager().getPlugin("ASkyBlock");
        if (aSkyBlock != null && aSkyBlock.isEnabled()) {
            try {
                managers.add(new ASkyBlockHook(aSkyBlock));
                Fawe.debug("Attempting to use plugin  'ASkyBlock' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (Settings.IMP.EXPERIMENTAL.FREEBUILD) {
            try {
                managers.add(new FreeBuildRegion());
                Fawe.debug("Attempting to use plugin '<internal.freebuild>'");
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
        String name = player.getName();
        com.sk89q.worldedit.entity.Player wePlayer = Fawe.get().getCachedPlayer(name);
        if (wePlayer != null) {
            wePlayer.unregister();
            Fawe.get().unregister(name);
        }
    }

    @Override
    public String getPlatform() {
        return "bukkit";
    }

    @Override
    public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    private boolean enabledBlocksHub = true;

    @Override
    public Object getBlocksHubApi() {
        if (!enabledBlocksHub) {
            return null;
        }
        Plugin blocksHubPlugin = Bukkit.getPluginManager().getPlugin("BlocksHub");
        if (blocksHubPlugin == null) {
            enabledBlocksHub = false;
            return null;
        }
        return null;
//        return ((BlocksHubBukkit) blocksHubPlugin).getApi();
    }
}
