package com.boydti.fawe.bukkit.util;

import com.boydti.fawe.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Method;

public class BukkitReflectionUtils {
    /**
     * prefix of bukkit classes
     */
    private static volatile String preClassB = null;
    /**
     * prefix of minecraft classes
     */
    private static volatile String preClassM = null;

    /**
     * check server version and class names
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


    /**
     * Get class for name. Replace {nms} to net.minecraft.server.V*. Replace {cb} to org.bukkit.craftbukkit.V*. Replace
     * {nm} to net.minecraft
     *
     * @param classes possible class paths
     * @return RefClass object
     * @throws RuntimeException if no class found
     */
    public static ReflectionUtils.RefClass getRefClass(final String... classes) throws RuntimeException {
        if (preClassM == null) {
            init();
        }
        for (String className : classes) {
            try {
                className = className.replace("{cb}", preClassB).replace("{nms}", preClassM).replace("{nm}", "net.minecraft");
                return ReflectionUtils.getRefClass(Class.forName(className));
            } catch (final ClassNotFoundException ignored) {
            }
        }
        throw new RuntimeException("no class found: " + classes[0].replace("{cb}", preClassB).replace("{nms}", preClassM).replace("{nm}", "net.minecraft"));
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
