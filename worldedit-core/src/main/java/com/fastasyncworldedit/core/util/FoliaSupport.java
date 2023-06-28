package com.fastasyncworldedit.core.util;

public final class FoliaSupport {
    private FoliaSupport() {

    }

    private static final boolean IS_FOLIA;
    private static final Class<?> TICK_THREAD_CLASS;
    static {
        boolean isFolia = false;
        try {
            // Assume API is present
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            isFolia = true;
        } catch (Exception unused) {

        }
        IS_FOLIA = isFolia;
        Class<?> tickThreadClass = String.class; // thread will never be instance of String
        if (IS_FOLIA) {
            try {
                tickThreadClass = Class.forName("io.papermc.paper.util.TickThread");
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
        TICK_THREAD_CLASS = tickThreadClass;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static boolean isTickThread() {
        return TICK_THREAD_CLASS.isInstance(Thread.currentThread());
    }
}
