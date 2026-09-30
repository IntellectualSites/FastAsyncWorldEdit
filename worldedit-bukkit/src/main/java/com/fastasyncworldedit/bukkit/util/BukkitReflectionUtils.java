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
        } else {
            preClassB = "org.bukkit.craftbukkit";
        }
        try {
            final Method getHandle = bukkitServerClass.getDeclaredMethod("getHandle");
            final Object handle = getHandle.invoke(server);
            final Class<?> handleServerClass = handle.getClass();
            pas = handleServerClass.getName().split("\\.");
            if (pas.length == 5) {
                final String verM = pas[3];
                preClassM = "net.minecraft.server." + verM;
            } else {
                preClassM = "net.minecraft.server";
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> getNmsClass(final String name) {
        String ver = getVersion();
        if (ver != null && !ver.isEmpty() && !ver.equals("craftbukkit")) {
            Class<?> clazz = ReflectionUtils.getClass("net.minecraft.server." + ver + "." + name);
            if (clazz != null) {
                return clazz;
            }
        }
        return ReflectionUtils.getClass("net.minecraft.server." + name);
    }

    public static Class<?> getCbClass(final String name) {
        String ver = getVersion();
        if (ver != null && !ver.isEmpty() && !ver.equals("craftbukkit")) {
            Class<?> clazz = ReflectionUtils.getClass("org.bukkit.craftbukkit." + ver + "." + name);
            if (clazz != null) {
                return clazz;
            }
        }
        return ReflectionUtils.getClass("org.bukkit.craftbukkit." + name);
    }

    public static String getVersion() {
        final String packageName = Bukkit.getServer().getClass().getPackage().getName();
        if (packageName.equals("org.bukkit.craftbukkit")) {
            return "";
        }
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

}
