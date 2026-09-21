package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.FoliaSupport;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

public class FoliaTaskManager extends TaskManager {

    private final static MethodHandle IS_GLOBAL_TICK_THREAD;

    static {
        try {
            IS_GLOBAL_TICK_THREAD = lookup().findStatic(Bukkit.class, "isGlobalTickThread", methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
          throw new AssertionError("Incompatile Folia version", e);
        }
    }

    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final Map<Integer, ScheduledTask> tasks = new ConcurrentHashMap<>();

    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        int id = idCounter.getAndIncrement();
        ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                scheduledTask -> {
                    if (scheduledTask.isCancelled()) {
                        tasks.remove(id);
                        return;
                    }
                    runnable.run();
                },
                ticksToMs(interval),
                ticksToMs(interval),
                TimeUnit.MILLISECONDS
        );
        tasks.put(id, task);
        return id;
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
    public void taskGlobal(final Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(WorldEditPlugin.getInstance(), asConsumer(runnable));
    }

    @Override
    public void later(@NotNull final Runnable runnable, final Location location, final int delay) {
        if (delay <= 0) {
            task(runnable, (World) location.getExtent(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
            return;
        }
        Bukkit.getRegionScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                BukkitAdapter.adapt(location),
                asConsumer(runnable),
                delay
        );
    }

    @Override
    public void laterGlobal(@NotNull final Runnable runnable, final int delay) {
        if (delay <= 0) {
            taskGlobal(runnable);
            return;
        }
        Bukkit.getGlobalRegionScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                delay
        );
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        if (delay <= 0) {
            async(runnable);
            return;
        }
        Bukkit.getAsyncScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                ticksToMs(delay),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            ScheduledTask scheduledTask = tasks.remove(task);
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }
    }

    @Override
    public <T> T syncAt(final Supplier<T> supplier, final World world, final int chunkX, final int chunkZ) {
        final org.bukkit.World adapt = BukkitAdapter.adapt(world);
        if (Bukkit.isOwnedByCurrentRegion(adapt, chunkX, chunkZ)) {
            return supplier.get();
        }
        ensureOffTickThread();
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Bukkit.getRegionScheduler().run(
                WorldEditPlugin.getInstance(),
                adapt,
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
        final org.bukkit.entity.Player adapt = BukkitAdapter.adapt(context);
        if (Bukkit.isOwnedByCurrentRegion(adapt)) {
            return supplier.get();
        }
        ensureOffTickThread();
        FutureTask<T> task = new FutureTask<>(supplier::get);
        adapt.getScheduler().execute(WorldEditPlugin.getInstance(), task, null, 0);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T syncGlobal(final Supplier<T> supplier) {
        if (isGlobalTickThread()) {
            return supplier.get();
        }
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Bukkit.getGlobalRegionScheduler().run(WorldEditPlugin.getInstance(), asConsumer(task));
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isGlobalTickThread() {
        return FoliaSupport.getRethrowing(() -> (boolean) IS_GLOBAL_TICK_THREAD.invokeExact());
    }

    private void ensureOffTickThread() {
        if (FoliaSupport.isTickThread()) {
            throw new IllegalStateException("Expected to be off tick thread");
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
