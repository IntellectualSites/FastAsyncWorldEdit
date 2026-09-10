package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemUtil {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final AtomicBoolean memory = new AtomicBoolean(false);
    private static final AtomicBoolean slower = new AtomicBoolean(false);

    public static boolean isMemoryFree() {
        return !memory.get();
    }

    public static boolean isMemoryLimited() {
        return memory.get();
    }

    public static boolean isMemoryLimitedSlow() {
        if (memory.get()) {
            System.gc();
            System.gc();
            calculateMemory();
            return memory.get();
        }
        return false;
    }

    public static boolean shouldBeginSlow() {
        return slower.get();
    }

    public static long getUsedBytes() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public static long getFreeBytes() {
        return Runtime.getRuntime().maxMemory() - getUsedBytes();
    }

    public static int calculateMemory() {
        final long heapSize = Runtime.getRuntime().totalMemory();
        final long heapMaxSize = Runtime.getRuntime().maxMemory();
        if (heapSize < heapMaxSize) {
            return Integer.MAX_VALUE;
        }
        final long heapFreeSize = Runtime.getRuntime().freeMemory();
        final int size = (int) ((heapFreeSize * 100) / heapMaxSize);
        if (size > (100 - Settings.settings().MAX_MEMORY_PERCENT)) {
            memoryPlentifulTask();
            return Integer.MAX_VALUE;
        }
        return size;
    }

    public static void checkAndSetApproachingLimit() {
        final long heapFreeSize = Runtime.getRuntime().freeMemory();
        final long heapMaxSize = Runtime.getRuntime().maxMemory();
        final int size = (int) ((heapFreeSize * 100) / heapMaxSize);
        boolean limited = size >= Settings.settings().SLOWER_MEMORY_PERCENT;
        slower.set(limited);
    }

    private static final Queue<Runnable> memoryLimitedTasks = new ConcurrentLinkedQueue<>();
    private static final Queue<Runnable> memoryPlentifulTasks = new ConcurrentLinkedQueue<>();

    public static void addMemoryLimitedTask(Runnable run) {
        if (run != null) {
            memoryLimitedTasks.add(run);
        }
    }

    public static void addMemoryPlentifulTask(Runnable run) {
        if (run != null) {
            memoryPlentifulTasks.add(run);
        }
    }

    public static void memoryLimitedTask() {
        System.gc();
        System.gc();
        for (Runnable task : memoryLimitedTasks) {
            task.run();
        }
        memory.set(true);
    }

    public static void memoryPlentifulTask() {
        for (Runnable task : memoryPlentifulTasks) {
            task.run();
        }
        memory.set(false);
    }

}
