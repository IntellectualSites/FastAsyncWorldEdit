package com.boydti.fawe.bukkit.util;

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionCheck {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static int checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        final Matcher matcher = Pattern.compile("(?:1\\.)?(\\d+)").matcher(javaVersion);
        if (!matcher.find()) {
            LOGGER.warn("Failed to determine Java version; Could not parse: {}", javaVersion);
            return -1;
        }

        final String version = matcher.group(1);
        try {
            return Integer.parseInt(version);
        } catch (final NumberFormatException e) {
            LOGGER.warn("Failed to determine Java version; Could not parse {} from {}", version, javaVersion, e);
            return -1;
        }
    }

    public static void checkJvm() {
        if (checkJavaVersion() < 11) {
            LOGGER.warn("************************************************************");
            LOGGER.warn("* WARNING - YOU ARE RUNNING AN OUTDATED VERSION OF JAVA.");
            LOGGER.warn("* FASTASYNCWORLDEDIT WILL STOP BEING COMPATIBLE WITH THIS VERSION OF");
            LOGGER.warn("* JAVA WHEN MINECRAFT 1.17 IS RELEASED.");
            LOGGER.warn("*");
            LOGGER.warn("* Please update the version of Java to 11. When Minecraft 1.17");
            LOGGER.warn("* is released, support for versions of Java prior to 11 will");
            LOGGER.warn("* be dropped.");
            LOGGER.warn("*");
            LOGGER.warn("* Current Java version: {}", System.getProperty("java.version"));
            LOGGER.warn("************************************************************");
        }
        if (checkJavaVersion() >= 15) {
            LOGGER.warn("************************************************************");
            LOGGER.warn("* FastAsyncWorldEdit uses Nashorn for the craftscript engine.");
            LOGGER.warn("* Within Java 15, Nashorn has been removed from Java.");
            LOGGER.warn("* Until we add a suitable workaround, you should stick to Java 11");
            LOGGER.warn("* to use all features of FastAsyncWorldEdit.");
            LOGGER.warn("************************************************************");
        }
    }
}
