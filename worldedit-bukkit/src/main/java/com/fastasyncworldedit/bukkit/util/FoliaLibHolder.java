package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.Fawe;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the FoliaLib instance for the plugin.
 * This class provides centralized access to FoliaLib functionality.
 */
public final class FoliaLibHolder {

    private static FoliaLib foliaLib;

    private FoliaLibHolder() {
        // Utility class
    }

    /**
     * Initialize the FoliaLib instance with the plugin.
     * This should be called once during plugin startup.
     *
     * @param plugin The plugin instance
     */
    public static void init(@NotNull Plugin plugin) {
        if (foliaLib == null) {
            foliaLib = new FoliaLib(plugin);
            // Set the Folia flag in Fawe for core module to use
            Fawe.setFoliaServer(foliaLib.isFolia());
        }
    }

    /**
     * Get the FoliaLib instance.
     *
     * @return The FoliaLib instance, or null if not initialized
     */
    @Nullable
    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    /**
     * Get the scheduler from FoliaLib.
     *
     * @return The platform scheduler
     * @throws IllegalStateException if FoliaLib is not initialized
     */
    @NotNull
    public static PlatformScheduler getScheduler() {
        if (foliaLib == null) {
            throw new IllegalStateException("FoliaLib has not been initialized. Call init() first.");
        }
        return foliaLib.getScheduler();
    }

    /**
     * Check if the server is running Folia.
     *
     * @return true if running on Folia, false otherwise
     */
    public static boolean isFolia() {
        if (foliaLib == null) {
            // Fall back to class detection if not initialized
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return foliaLib.isFolia();
    }

    /**
     * Check if the server is running Paper.
     *
     * @return true if running on Paper, false otherwise
     */
    public static boolean isPaper() {
        if (foliaLib == null) {
            return false;
        }
        return foliaLib.isPaper();
    }

    /**
     * Check if the server is running Spigot.
     *
     * @return true if running on Spigot, false otherwise
     */
    public static boolean isSpigot() {
        if (foliaLib == null) {
            return false;
        }
        return foliaLib.isSpigot();
    }

    /**
     * Shutdown and cleanup FoliaLib resources.
     * Should be called during plugin disable.
     */
    public static void shutdown() {
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
            foliaLib = null;
        }
    }

}