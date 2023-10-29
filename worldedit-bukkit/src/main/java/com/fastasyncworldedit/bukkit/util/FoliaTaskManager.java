package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FoliaTaskManager extends TaskManager {

    private final AtomicInteger idCounter = new AtomicInteger();

    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        // TODO return some kind of own ScheduledTask instead of int
        Bukkit.getAsyncScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                0,
                ticksToMs(interval),
                TimeUnit.MILLISECONDS
        );
        return idCounter.getAndIncrement();
    }

    @Override
    public void async(@NotNull final Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(WorldEditPlugin.getInstance(), asConsumer(runnable));
    }

    @Override
    public void task(@NotNull final Runnable runnable, @NotNull final World world, final int chunkX, final int chunkZ) {
        Bukkit.getRegionScheduler().run(
                WorldEditPlugin.getInstance(),
                BukkitAdapter.adapt(world),
                chunkX,
                chunkZ,
                asConsumer(runnable)
        );
    }

    @Override
    public void later(@NotNull final Runnable runnable, final Location location, final int delay) {
        Bukkit.getRegionScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                BukkitAdapter.adapt(location),
                asConsumer(runnable),
                delay
        );
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        Bukkit.getAsyncScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                ticksToMs(delay),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void cancel(final int task) {
        fail("Not implemented");
    }

    @Override
    public <T> T syncAt(final Supplier<T> supplier, final World world, final int chunkX, final int chunkZ) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Bukkit.getRegionScheduler().run(
                WorldEditPlugin.getInstance(),
                BukkitAdapter.adapt(world),
                chunkX,
                chunkZ,
                asConsumer(task)
        );
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private <R> Consumer<R> asConsumer(Runnable runnable) {
        return __ -> runnable.run();
    }

    @Override
    public <T> T syncWith(final Supplier<T> supplier, final Player context) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        BukkitAdapter.adapt(context)
                .getScheduler()
                .execute(WorldEditPlugin.getInstance(), task, null, 0);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T syncGlobal(final Supplier<T> supplier) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Bukkit.getGlobalRegionScheduler().run(WorldEditPlugin.getInstance(), asConsumer(task));
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int ticksToMs(int ticks) {
        // 1 tick = 50ms
        return ticks * 50;
    }


    private <T> T fail(String message) {
        throw new UnsupportedOperationException(message);
    }

}
