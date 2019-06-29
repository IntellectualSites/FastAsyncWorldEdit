package com.boydti.fawe.bukkit.util;

import com.boydti.fawe.util.TaskManager;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class BukkitTaskMan extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskMan(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@NotNull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    public MutableInt index = new MutableInt(0);

    @Override
    public void async(@NotNull final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable).getTaskId();
    }

    @Override
    public void task(@NotNull final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable).getTaskId();
    }

    @Override
    public void later(@NotNull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, delay).getTaskId();
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }
}
