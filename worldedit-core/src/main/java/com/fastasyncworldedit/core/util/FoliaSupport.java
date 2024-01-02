package com.fastasyncworldedit.core.util;

public final class FoliaSupport {
    private FoliaSupport() {

    }

    private static final boolean IS_FOLIA;
    private static final Class<?> TICK_THREAD_CLASS;
    static {
        boolean isFolia = false;
        try {
            // Assume implementation details are present
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
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


    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Throwable;

    }
    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Throwable;

    }

    public static void runRethrowing(ThrowingRunnable runnable) {
        getRethrowing(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T getRethrowing(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
