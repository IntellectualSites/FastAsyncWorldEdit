package com.fastasyncworldedit.nukkit.adapter;

/**
 * Detects the running Nukkit platform (MOT vs NKX) and loads the appropriate adapter.
 */
public final class NukkitImplLoader {

    private static NukkitImplAdapter instance;

    private NukkitImplLoader() {
    }

    /**
     * Detect and load the platform adapter. Caches the result for subsequent calls.
     *
     * @return the loaded adapter
     * @throws RuntimeException if no adapter could be loaded
     */
    public static NukkitImplAdapter detect() {
        if (instance != null) {
            return instance;
        }
        synchronized (NukkitImplLoader.class) {
            if (instance != null) {
                return instance;
            }
            instance = doDetect();
            return instance;
        }
    }

    /**
     * Get the cached adapter instance. Must call {@link #detect()} first.
     */
    public static NukkitImplAdapter get() {
        if (instance == null) {
            throw new IllegalStateException("NukkitImplLoader.detect() has not been called yet");
        }
        return instance;
    }

    private static NukkitImplAdapter doDetect() {
        // Detect Nukkit-MOT by checking for a MOT-specific class
        boolean isMot;
        try {
            Class.forName("cn.nukkit.GameVersion");
            isMot = true;
        } catch (ClassNotFoundException e) {
            isMot = false;
        }

        String className = isMot
                ? "com.fastasyncworldedit.nukkit.adapter.mot.MotNukkitAdapter"
                : "com.fastasyncworldedit.nukkit.adapter.nkx.NkxNukkitAdapter";

        try {
            Class<?> clazz = Class.forName(className);
            return (NukkitImplAdapter) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to load Nukkit adapter: " + className, e);
        }
    }

}
