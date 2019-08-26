package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.bukkit.adapter.BukkitQueueHandler;
import com.boydti.fawe.bukkit.listener.BrushListener;
import com.boydti.fawe.bukkit.listener.BukkitImageListener;
import com.boydti.fawe.bukkit.listener.RenderListener;
import com.boydti.fawe.bukkit.regions.*;
import com.boydti.fawe.bukkit.util.BukkitTaskMan;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.bukkit.util.VaultUtil;
import com.boydti.fawe.bukkit.util.image.BukkitImageViewer;
import com.boydti.fawe.bukkit.listener.ChunkListener_8;
import com.boydti.fawe.bukkit.listener.ChunkListener_9;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.Jars;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.image.ImageViewer;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import org.bstats.bukkit.MetricsLite;
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

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

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
            if (Bukkit.getVersion().contains("git-Spigot")) {
                debug("====== USE PAPER ======");
                debug("DOWNLOAD: https://papermc.io/ci/job/Paper-1.13/");
                debug("GUIDE: https://www.spigotmc.org/threads/21726/");
                debug(" - This is only a recommendation");
                debug("==============================");
            }
            if (Bukkit.getVersion().contains("git-Paper") && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
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
    public synchronized ImageViewer getImageViewer(FawePlayer fp) {
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
            BukkitImageViewer viewer = new BukkitImageViewer(BukkitAdapter.adapt(fp.toWorldEditPlayer()));
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
    public FawePlayer<Player> wrap(final Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            Player player = Bukkit.getPlayer(name);
            return player != null ? new BukkitPlayer(player) : null;
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new BukkitPlayer(player);
        } else if (obj.getClass().getName().contains("EntityPlayer")) {
            try {
                Method method = obj.getClass().getDeclaredMethod("getBukkitEntity");
                return wrap(method.invoke(obj));
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override public void startMetrics() {
        new MetricsLite(plugin);
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
                Fawe.debug("Plugin 'WorldGuard' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin));
                Fawe.debug("Plugin 'Towny' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin factionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
        if (factionsPlugin != null && factionsPlugin.isEnabled()) {
            try {
                managers.add(new FactionsFeature(factionsPlugin));
                Fawe.debug("Plugin 'Factions' found. Using it now.");
            } catch (Throwable e) {
                try {
                    managers.add(new FactionsUUIDFeature(factionsPlugin, this));
                    Fawe.debug("Plugin 'FactionsUUID' found. Using it now.");
                } catch (Throwable e2) {
                    try {
                        managers.add(new FactionsOneFeature(factionsPlugin));
                        Fawe.debug("Plugin 'FactionsUUID' found. Using it now.");
                    } catch (Throwable e3) {
                        e.printStackTrace();
                    }

                }
            }
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if (residencePlugin != null && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                Fawe.debug("Plugin 'Residence' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                Fawe.debug("Plugin 'GriefPrevention' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin preciousStonesPlugin = Bukkit.getServer().getPluginManager().getPlugin("PreciousStones");
        if (preciousStonesPlugin != null && preciousStonesPlugin.isEnabled()) {
            try {
                managers.add(new PreciousStonesFeature(preciousStonesPlugin, this));
                Fawe.debug("Plugin 'PreciousStones' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }


        final Plugin aSkyBlock = Bukkit.getServer().getPluginManager().getPlugin("ASkyBlock");
        if (aSkyBlock != null && aSkyBlock.isEnabled()) {
            try {
                managers.add(new ASkyBlockHook(aSkyBlock));
                Fawe.debug("Plugin 'ASkyBlock' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (Settings.IMP.EXPERIMENTAL.FREEBUILD) {
            try {
                managers.add(new FreeBuildRegion());
                Fawe.debug("Plugin '<internal.freebuild>' found. Using it now.");
            } catch (Throwable e) {
                e.printStackTrace();
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
        FawePlayer fp = Fawe.get().getCachedPlayer(name);
        if (fp != null) {
            fp.unregister();
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
