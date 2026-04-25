package com.fastasyncworldedit.nukkit.adapter;

import java.util.Set;

/**
 * Detects the running Nukkit platform (MOT vs NKX) and loads the appropriate adapter.
 * <p>
 * Nukkit does not have a clean versioned API like Bukkit's NMS packages
 * ({@code net.minecraft.server.v1_20_R3}). Both MOT and NKX use {@code cn.nukkit}, so
 * we cannot compile against both simultaneously. Instead, we detect the
 * active fork at runtime using {@code Class.forName("cn.nukkit.GameVersion")},
 * which only exists in MOT. The matching adapter class is then loaded
 * reflectively from its version-specific module.
 * <p>
 * This approach avoids classpath conflicts and allows a single FAWE JAR
 * to support both forks without shading or relocation.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit adapters are selected by MC version at compile time; Nukkit by fork at runtime</li>
 *   <li>Reflection is required for all fork-specific class access</li>
 * </ul>
 *
 * @see NukkitImplAdapter
 */
public final class NukkitImplLoader {

    private static NukkitImplAdapter instance;
    private static String platformVersion;
    private static Set<NukkitPlatformCapabilities> capabilities;

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
            cacheMetadata(instance);
            return instance;
        }
        synchronized (NukkitImplLoader.class) {
            if (instance != null) {
                cacheMetadata(instance);
                return instance;
            }
            instance = doDetect();
            cacheMetadata(instance);
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

    public static String getPlatformVersion() {
        NukkitImplAdapter adapter = get();
        cacheMetadata(adapter);
        return platformVersion;
    }

    public static Set<NukkitPlatformCapabilities> getCapabilities() {
        NukkitImplAdapter adapter = get();
        cacheMetadata(adapter);
        return capabilities;
    }

    public static boolean supports(NukkitPlatformCapabilities capability) {
        return getCapabilities().contains(capability);
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

    private static void cacheMetadata(NukkitImplAdapter adapter) {
        platformVersion = adapter.getVersion();
        capabilities = Set.copyOf(adapter.getCapabilities());
    }

}
