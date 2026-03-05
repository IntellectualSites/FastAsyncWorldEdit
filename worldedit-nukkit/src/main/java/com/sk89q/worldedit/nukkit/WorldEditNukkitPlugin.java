package com.sk89q.worldedit.nukkit;

import cn.nukkit.plugin.PluginBase;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.nukkit.FaweNukkit;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.weather.WeatherTypes;

/**
 * FastAsyncWorldEdit plugin entry point for Nukkit.
 */
public class WorldEditNukkitPlugin extends PluginBase {

    private static WorldEditNukkitPlugin instance;
    private NukkitConfiguration configuration;
    private NukkitServerInterface platform;

    public static WorldEditNukkitPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        // Detect and load platform adapter (MOT vs NKX)
        NukkitImplAdapter adapter = NukkitImplLoader.detect();
        getLogger().info("Detected Nukkit platform: " + adapter.getPlatformName());

        // Create data folder
        getDataFolder().mkdirs();

        // Load configuration before registering platform
        loadConfiguration();

        // Register platform with WorldEdit
        WorldEdit worldEdit = WorldEdit.getInstance();
        platform = new NukkitServerInterface(this, getServer());
        worldEdit.getPlatformManager().register(platform);
    }

    @Override
    public void onEnable() {
        // Check SQLite JDBC driver (soft dependency)
        if (!checkSQLiteDriver()) {
            return;
        }

        // Initialize FAWE
        new FaweNukkit(this);

        WorldEdit worldEdit = WorldEdit.getInstance();

        // Signal that all platforms are registered
        worldEdit.getEventBus().post(new PlatformsRegisteredEvent());

        // Pre-load JE block/item IDs (needed by BlockTypesCache/ItemTypesCache during loadMappings)
        BlockMapping.initJeBlockDefaults();
        ItemMapping.initJeItemIds();

        // Load bundled mappings (block data, item data, legacy mapper)
        worldEdit.loadMappings();

        // Initialize Nukkit-specific mappings (JE <-> BE block state mapping)
        BlockMapping.init();
        BiomeMapping.init();
        ItemMapping.init();

        // Initialize registries
        initializeRegistries();

        // Register event listeners
        getServer().getPluginManager().registerEvents(
                new NukkitWorldEditListener(this), this
        );

        // Build ordinal mappings (must be after BlockTypesCache is initialized)
        BlockMapping.buildOrdinalMappings();

        // Signal platform is ready
        worldEdit.getEventBus().post(new PlatformReadyEvent(platform));

        getLogger().info("FastAsyncWorldEdit for Nukkit enabled.");
    }

    @Override
    public void onDisable() {
        // FAWE cleanup (may be null if onEnable failed early)
        Fawe fawe = Fawe.instance();
        if (fawe != null) {
            fawe.onDisable();
        }

        WorldEdit worldEdit = WorldEdit.getInstance();

        // Unload sessions
        worldEdit.getSessionManager().unload();

        // Signal platform is going down and unregister
        if (platform != null) {
            worldEdit.getEventBus().post(new PlatformUnreadyEvent(platform));
            worldEdit.getPlatformManager().unregister(platform);
        }

        // Cancel scheduled tasks
        getServer().getScheduler().cancelTask(this);

        getLogger().info("FastAsyncWorldEdit for Nukkit disabled.");
    }

    private boolean checkSQLiteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            getLogger().error("===========================================");
            getLogger().error("SQLite JDBC driver not found!");
            getLogger().error("Please download DbLib.jar and put it");
            getLogger().error("in the server's 'plugins' folder.");
            getLogger().error("Download: https://cloudburstmc.org/resources/dblib.12/");
            getLogger().error("===========================================");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void initializeRegistries() {
        // Initialize biome registry with known biome types
        for (String biomeId : BiomeMapping.getAllJeBiomes()) {
            if (BiomeType.REGISTRY.get(biomeId) == null) {
                BiomeTypes.register(new BiomeType(biomeId));
            }
        }

        // Initialize game modes and weather types
        GameModes.get("");
        WeatherTypes.get("");
    }

    public void loadConfiguration() {
        configuration = new NukkitConfiguration(this);
        configuration.load();
    }

    public LocalConfiguration getLocalConfiguration() {
        return configuration;
    }

    NukkitServerInterface getInternalPlatform() {
        return platform;
    }

}
