package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemUtil {

    private static AtomicBoolean memory = new AtomicBoolean(false);

    public static boolean isMemoryFree() {
        return !memory.get();
    }

    public static boolean isMemoryLimited() {
        return memory.get();
    }

    public static boolean isMemoryLimitedSlow() {
        if (memory.get()) {
            FaweCache.cleanAll();
            System.gc();
            System.gc();
            calculateMemory();
            return memory.get();
        }
        return false;
    }

    public static long getUsedBytes() {
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return used;
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
        if (size > (100 - Settings.IMP.MAX_MEMORY_PERCENT)) {
            memoryPlentifulTask();
            return Integer.MAX_VALUE;
        }
        return size;
    }

    private static Queue<Runnable> memoryLimitedTasks = new ConcurrentLinkedQueue<>();
    private static Queue<Runnable> memoryPlentifulTasks = new ConcurrentLinkedQueue<>();

    public static void addMemoryLimitedTask(Runnable run) {
        if (run != null)
            memoryLimitedTasks.add(run);
    }

    public static void addMemoryPlentifulTask(Runnable run) {
        if (run != null)
            memoryPlentifulTasks.add(run);
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
