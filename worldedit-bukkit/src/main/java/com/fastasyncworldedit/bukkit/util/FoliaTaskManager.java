package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.dropReturn;
import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

public class FoliaTaskManager extends TaskManager {

    private final ScheduledExecutorService backgroundExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger idCounter = new AtomicInteger();

    @Override
    public int repeat(@NotNull final Runnable runnable, final int interval) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                scheduledTask -> runnable.run(),
                1,
                interval
        );
        return 0;
    }

    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        Bukkit.getAsyncScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                scheduledTask -> runnable.run(),
                0,
                ticksToMs(interval),
                TimeUnit.MILLISECONDS
        );
        return idCounter.getAndIncrement();
    }

    @Override
    public void async(@NotNull final Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(WorldEditPlugin.getInstance(), (s) -> runnable.run());
    }

    @Override
    public void task(@NotNull final Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(WorldEditPlugin.getInstance(), runnable);
    }

    @Override
    public void task(@NotNull final Runnable runnable, @NotNull final Location context) {
        SchedulerAdapter.executeForLocation(context, runnable);
    }

    @Override
    public void later(@NotNull final Runnable runnable, final int delay) {
        Bukkit.getGlobalRegionScheduler().runDelayed(WorldEditPlugin.getInstance(), scheduledTask -> runnable.run(), delay);
    }

    @Override
    public void later(@NotNull final Runnable runnable, final Location location, final int delay) {
        fail("Not implemented");
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        backgroundExecutor.schedule(runnable, ticksToMs(delay), TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancel(final int task) {
        fail("Not implemented");
    }

    @Override
    public <T> T syncAt(final Supplier<T> supplier, final Location context) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        SchedulerAdapter.executeForLocation(context, task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T syncAt(final Supplier<T> supplier, final World context, int chunkX, int chunkZ) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        SchedulerAdapter.executeForChunk(context,chunkX, chunkZ, task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T syncWith(final Supplier<T> supplier, final Player context) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        SchedulerAdapter.executeForEntity(context, task);
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

    private <T> T fail() {
        return fail("No main thread present");
    }

    private <T> T fail(String message) {
        throw new UnsupportedOperationException(message);
    }

    private static class SchedulerAdapter {

        private static final MethodHandle EXECUTE_FOR_LOCATION;
        private static final MethodHandle EXECUTE_FOR_CHUNK;
        private static final MethodHandle EXECUTE_FOR_PLAYER;
        private static final Runnable THROW_IF_RETIRED = () -> throwRetired();

        private static final MethodType LOCATION_EXECUTE_TYPE = methodType(
                void.class,
                Plugin.class,
                org.bukkit.Location.class,
                Runnable.class
        );

        private static final MethodType CHUNK_EXECUTE_TYPE = methodType(
                void.class,
                Plugin.class,
                org.bukkit.World.class,
                int.class,
                int.class,
                Runnable.class
        );

        private static final MethodType ENTITY_EXECUTE_TYPE = methodType(
                boolean.class,
                Plugin.class,
                Runnable.class,
                Runnable.class,
                long.class
        );

        static {
            final Plugin pluginInstance = WorldEditPlugin.getInstance();
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodHandle executeForLocation;
            MethodHandle executeForChunk;

            MethodHandle executeForPlayer;
            try {
                Class<?> regionisedSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaRegionScheduler");
                final Method method = Bukkit.class.getDeclaredMethod("getRegionScheduler");
                executeForLocation = lookup.findVirtual(
                        regionisedSchedulerClass,
                        "execute",
                        LOCATION_EXECUTE_TYPE
                );
                executeForLocation = executeForLocation.bindTo(method.invoke(null));
                executeForLocation = executeForLocation.bindTo(pluginInstance);

                executeForChunk = lookup.findVirtual(
                        regionisedSchedulerClass,
                        "execute",
                        CHUNK_EXECUTE_TYPE
                );
                executeForChunk = executeForChunk.bindTo(method.invoke(null));
                executeForChunk = executeForChunk.bindTo(pluginInstance);

                Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                executeForPlayer = lookup.findVirtual(
                        entitySchedulerClass,
                        "execute",
                        ENTITY_EXECUTE_TYPE
                );
                // (ES, P, R, R, L)Z (ES, R, R, L)Z
                executeForPlayer = insertArguments(executeForPlayer, 1, pluginInstance);
                // (ES, R1, R2, L)Z -> (ES, R1)Z
                executeForPlayer = insertArguments(executeForPlayer, 2, THROW_IF_RETIRED, 0);
                // (ES, R1)Z -> (ES, R1)V
                executeForPlayer = dropReturn(executeForPlayer);
                MethodHandle getScheduler = lookup.findVirtual(
                        org.bukkit.entity.Entity.class,
                        "getScheduler",
                        methodType(entitySchedulerClass)
                );
                // (ES, R1)V -> (E, R1)V
                executeForPlayer = filterArguments(executeForPlayer, 0, getScheduler);
                MethodType finalType = methodType(void.class, org.bukkit.entity.Player.class, Runnable.class);
                // (ES, R1)V -> (P, R1)V
                executeForPlayer = explicitCastArguments(executeForPlayer, finalType);
            } catch (Throwable throwable) {
                throw new AssertionError(throwable);
            }
            EXECUTE_FOR_LOCATION = executeForLocation;
            EXECUTE_FOR_CHUNK = executeForChunk;
            EXECUTE_FOR_PLAYER = executeForPlayer;
        }

        static void executeForChunk(World world,int chunkX, int chunkZ, Runnable task) {
            try {
                EXECUTE_FOR_CHUNK.invokeExact(world,chunkX, chunkZ, task);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable other) {
                throw new RuntimeException(other);
            }
        }

        static void executeForLocation(Location location, Runnable task) {
            try {
                EXECUTE_FOR_LOCATION.invokeExact(BukkitAdapter.adapt(location), task);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable other) {
                throw new RuntimeException(other);
            }
        }
        static void executeForEntity(Player player, Runnable task) {
            try {
                EXECUTE_FOR_PLAYER.invokeExact(BukkitAdapter.adapt(player), task);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable other) {
                throw new RuntimeException(other);
            }
        }

        private static void throwRetired() {
            throw new RuntimeException("Player retired");
        }

    }

}
