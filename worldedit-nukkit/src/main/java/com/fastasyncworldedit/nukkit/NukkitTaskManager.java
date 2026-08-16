package com.fastasyncworldedit.nukkit;

import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import com.fastasyncworldedit.core.util.TaskManager;

import javax.annotation.Nonnull;

public class NukkitTaskManager extends TaskManager {

    private final Plugin plugin;

    public NukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler()
                .scheduleRepeatingTask(this.plugin, runnable, interval).getTaskId();
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler()
                .scheduleRepeatingTask(this.plugin, runnable, interval, true).getTaskId();
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        this.plugin.getServer().getScheduler()
                .scheduleTask(this.plugin, runnable, true);
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        this.plugin.getServer().getScheduler()
                .scheduleTask(this.plugin, runnable);
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler()
                .scheduleDelayedTask(this.plugin, runnable, delay);
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler()
                .scheduleDelayedTask(this.plugin, runnable, delay, true);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Server.getInstance().getScheduler().cancelTask(task);
        }
    }

}
