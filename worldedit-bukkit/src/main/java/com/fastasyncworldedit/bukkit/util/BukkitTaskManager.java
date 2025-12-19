package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

/**
 * Task manager implementation for Bukkit/Paper/Folia servers.
 * Uses FoliaLib for cross-platform scheduler compatibility.
 */
public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    private PlatformScheduler getScheduler() {
        return FoliaLibHolder.getScheduler();
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        // FoliaLib handles the difference between Folia and non-Folia servers
        getScheduler().runTimer(runnable, interval, interval);
        // FoliaLib doesn't return task IDs in the same way, return 0 as a placeholder
        return 0;
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        getScheduler().runTimerAsync(runnable, interval, interval);
        return 0;
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        getScheduler().runAsync(task -> runnable.run());
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        getScheduler().runNextTick(task -> runnable.run());
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        if (delay <= 0) {
            getScheduler().runNextTick(task -> runnable.run());
        } else {
            getScheduler().runLater(runnable, delay);
        }
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        if (delay <= 0) {
            getScheduler().runAsync(task -> runnable.run());
        } else {
            getScheduler().runLaterAsync(runnable, delay);
        }
    }

    @Override
    public void cancel(final int task) {
        // FoliaLib manages tasks internally, and we don't have individual task IDs
        // This method is mostly a no-op with FoliaLib since it uses WrappedTask
        // For legacy compatibility, try to cancel via Bukkit scheduler on non-Folia
        // servers
        if (task != -1 && !FoliaLibHolder.isFolia()) {
            try {
                Bukkit.getScheduler().cancelTask(task);
            } catch (Exception ignored) {
                // May not be a valid task ID if it was scheduled via FoliaLib
            }
        }
    }

}
