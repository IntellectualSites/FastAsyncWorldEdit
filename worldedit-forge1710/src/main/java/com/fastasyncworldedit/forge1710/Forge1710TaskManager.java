package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.util.TaskManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sync tasks run on the server thread at the end of each server tick (1 tick = 50ms); async tasks run on a pool.
 */
public class Forge1710TaskManager extends TaskManager {

    private static final Logger LOGGER = LogManager.getLogger("FAWE-Forge1710");

    private final AtomicInteger ids = new AtomicInteger();
    private final ConcurrentLinkedQueue<Runnable> nextTick = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Scheduled> scheduled = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> asyncScheduled = new ConcurrentHashMap<>();
    private final ExecutorService asyncPool = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "FAWE Forge1710 async");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService asyncTimer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FAWE Forge1710 timer");
        thread.setDaemon(true);
        return thread;
    });

    private static final class Scheduled {

        final Runnable runnable;
        final int interval;
        long remaining;

        Scheduled(Runnable runnable, int delay, int interval) {
            this.runnable = runnable;
            this.remaining = Math.max(0, delay);
            this.interval = interval;
        }

    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Runnable task;
        while ((task = nextTick.poll()) != null) {
            run(task);
        }
        Iterator<Map.Entry<Integer, Scheduled>> it = scheduled.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Scheduled> entry = it.next();
            Scheduled s = entry.getValue();
            if (s.remaining-- > 0) {
                continue;
            }
            if (s.interval <= 0) {
                it.remove();
            } else {
                s.remaining = s.interval - 1;
            }
            run(s.runnable);
        }
    }

    private static void run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            LOGGER.error("Error running FAWE task", t);
        }
    }

    @Override
    public int repeat(@Nonnull Runnable runnable, int interval) {
        int id = ids.incrementAndGet();
        scheduled.put(id, new Scheduled(runnable, interval, Math.max(1, interval)));
        return id;
    }

    @Override
    public int repeatAsync(@Nonnull Runnable runnable, int interval) {
        int id = ids.incrementAndGet();
        long periodMs = Math.max(1, interval) * 50L;
        asyncScheduled.put(id, asyncTimer.scheduleAtFixedRate(() -> run(runnable), periodMs, periodMs, TimeUnit.MILLISECONDS));
        return id;
    }

    @Override
    public void async(@Nonnull Runnable runnable) {
        asyncPool.execute(() -> run(runnable));
    }

    @Override
    public void task(@Nonnull Runnable runnable) {
        nextTick.add(runnable);
    }

    @Override
    public void later(@Nonnull Runnable runnable, int delay) {
        int id = ids.incrementAndGet();
        scheduled.put(id, new Scheduled(runnable, delay, 0));
    }

    @Override
    public void laterAsync(@Nonnull Runnable runnable, int delay) {
        asyncTimer.schedule(() -> async(runnable), Math.max(0, delay) * 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancel(int task) {
        scheduled.remove(task);
        ScheduledFuture<?> future = asyncScheduled.remove(task);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void shutdown() {
        scheduled.clear();
        asyncScheduled.values().forEach(f -> f.cancel(false));
        asyncScheduled.clear();
        asyncTimer.shutdownNow();
        asyncPool.shutdown();
    }

}
