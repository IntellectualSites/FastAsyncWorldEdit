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
        Class<?> tickThreadClass = Void.class;
        if (IS_FOLIA) {
            String[] candidates = {
                "ca.spottedleaf.moonrise.common.util.TickThread", // Moonrise internals (Paper pre-1.21.4)
                "io.papermc.paper.util.TickThread",               // Paper 1.21.4+
                "io.papermc.paper.threadedregions.TickThread"      // Folia (legacy)
            };
            for (String candidate : candidates) {
                try {
                    tickThreadClass = Class.forName(candidate);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
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
