package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable).getTaskId();
    }

    @Override
    public void task(@NotNull final Runnable runnable, @NotNull final Location location) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public void task(@NotNull final Runnable runnable, @NotNull final World world, final int chunkX, final int chunkZ) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public void taskGlobal(final Runnable runnable) {
        this.plugin.getServer().getGlobalRegionScheduler().run(this.plugin, task -> runnable.run());
    }

    @Override
    public void later(@NotNull final Runnable runnable, final Location location, final int delay) {
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, delay);
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }

    // TODO

    @Override
    public <T> T syncAt(final Supplier<T> supplier, final World world, final int chunkX, final int chunkZ) {
        return sync(supplier);
    }

    @Override
    public <T> T syncWith(final Supplier<T> supplier, final Player context) {
        return sync(supplier);
    }

    @Override
    public <T> T syncGlobal(final Supplier<T> supplier) {
        return sync(supplier);
    }

    private <T> T sync(final Supplier<T> supplier) {
        if (Fawe.isTickThread()) {
            return supplier.get();
        }
        try {
            // TODO
            return Fawe.instance().getQueueHandler().sync(supplier).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
