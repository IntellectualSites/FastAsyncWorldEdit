package com.boydti.fawe.bukkit.util;

import com.boydti.fawe.util.TaskManager;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitTaskMan extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskMan(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(final Runnable r, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, r, interval, interval);
    }

    @Override
    public int repeatAsync(final Runnable r, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, r, interval, interval);
    }

    public MutableInt index = new MutableInt(0);

    @Override
    public void async(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, r).getTaskId();
    }

    @Override
    public void task(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, r).getTaskId();
    }

    @Override
    public void later(final Runnable r, final int delay) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, r, delay).getTaskId();
    }

    @Override
    public void laterAsync(final Runnable r, final int delay) {
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, r, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }
}
