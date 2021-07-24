package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Method;

public class BukkitReflectionUtils {

    /**
     * Prefix of Bukkit classes.
     */
    private static volatile String preClassB = null;
    /**
     * Prefix of Minecraft classes.
     */
    private static volatile String preClassM = null;

    /**
     * Check server version and class names.
     */
    public static void init() {
        final Server server = Bukkit.getServer();
        final Class<?> bukkitServerClass = server.getClass();
        String[] pas = bukkitServerClass.getName().split("\\.");
        if (pas.length == 5) {
            final String verB = pas[3];
            preClassB = "org.bukkit.craftbukkit." + verB;
        }
        try {
            final Method getHandle = bukkitServerClass.getDeclaredMethod("getHandle");
            final Object handle = getHandle.invoke(server);
            final Class<?> handleServerClass = handle.getClass();
            pas = handleServerClass.getName().split("\\.");
            if (pas.length == 5) {
                final String verM = pas[3];
                preClassM = "net.minecraft.server." + verM;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> getNmsClass(final String name) {
        final String className = "net.minecraft.server." + getVersion() + "." + name;
        return ReflectionUtils.getClass(className);
    }

    public static Class<?> getCbClass(final String name) {
        final String className = "org.bukkit.craftbukkit." + getVersion() + "." + name;
        return ReflectionUtils.getClass(className);
    }

    public static String getVersion() {
        final String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

}
