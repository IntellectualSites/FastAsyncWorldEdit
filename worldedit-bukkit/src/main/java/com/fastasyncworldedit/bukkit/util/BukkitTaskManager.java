package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.bukkit.util.scheduler.Scheduler;
import com.fastasyncworldedit.core.util.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;
    private final Scheduler scheduler;

    public BukkitTaskManager(final Plugin plugin, final Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        this.scheduler.runTaskTimer(this.plugin, runnable, interval, interval);
        return 0;
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        this.scheduler.runTaskTimer(this.plugin, runnable, interval, interval);
        return 0;
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        this.scheduler.runTaskAsynchronously(this.plugin, runnable);
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        this.scheduler.runTask(this.plugin, runnable);
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        this.scheduler.runTaskLater(this.plugin, runnable, delay);
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        this.scheduler.runTaskLaterAsynchronously(this.plugin, runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            try {
                Bukkit.getScheduler().cancelTask(task);
            } catch (Exception e) {
                // Ignore errors on Folia - task cancellation by ID is not supported
            }
        }
    }

}
