package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.FoliaUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;
    private final Map<Integer, ScheduledTask> foliaTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger(1);

    public BukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        if (FoliaUtil.isFoliaServer()) {
            int taskId = taskIdCounter.getAndIncrement();
            ScheduledTask task = this.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                    this.plugin,
                    scheduledTask -> {
                        if (scheduledTask.isCancelled()) {
                            foliaTasks.remove(taskId);
                            return;
                        }
                        runnable.run();
                    },
                    interval,
                    interval
            );
            foliaTasks.put(taskId, task);
            return taskId;
        }
        return this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        if (FoliaUtil.isFoliaServer()) {
            int taskId = taskIdCounter.getAndIncrement();
            ScheduledTask task = this.plugin.getServer().getAsyncScheduler().runAtFixedRate(
                    this.plugin,
                    scheduledTask -> {
                        if (scheduledTask.isCancelled()) {
                            foliaTasks.remove(taskId);
                            return;
                        }
                        runnable.run();
                    },
                    interval * 50L,
                    interval * 50L,
                    TimeUnit.MILLISECONDS
            );
            foliaTasks.put(taskId, task);
            return taskId;
        }
        return this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        if (FoliaUtil.isFoliaServer()) {
            this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, scheduledTask -> runnable.run());
            return;
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable).getTaskId();
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        if (FoliaUtil.isFoliaServer()) {
            this.plugin.getServer().getGlobalRegionScheduler().run(this.plugin, scheduledTask -> runnable.run());
            return;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable).getTaskId();
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        if (FoliaUtil.isFoliaServer()) {
            if (delay == 0) {
                this.plugin.getServer().getGlobalRegionScheduler().run(this.plugin, scheduledTask -> runnable.run());
            } else {
                this.plugin.getServer().getGlobalRegionScheduler().runDelayed(this.plugin, scheduledTask -> runnable.run(), delay);
            }
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, delay).getTaskId();
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        if (FoliaUtil.isFoliaServer()) {
            if (delay == 0) {
                this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, scheduledTask -> runnable.run());
            } else {
                this.plugin.getServer().getAsyncScheduler().runDelayed(this.plugin, scheduledTask -> runnable.run(), delay * 50L, TimeUnit.MILLISECONDS);
            }
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            if (FoliaUtil.isFoliaServer()) {
                ScheduledTask scheduledTask = foliaTasks.remove(task);
                if (scheduledTask != null) {
                    scheduledTask.cancel();
                }
                return;
            }
            Bukkit.getScheduler().cancelTask(task);
        }
    }

}
