package com.boydti.fawe.bukkit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionCheck {

    public static final Logger logger = LoggerFactory.getLogger(JavaVersionCheck.class);

    private static int checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        final Matcher matcher = Pattern.compile("(?:1\\.)?(\\d+)").matcher(javaVersion);
        if (!matcher.find()) {
            logger.warn("Failed to determine Java version; Could not parse: {}", javaVersion);
            return -1;
        }

        final String version = matcher.group(1);
        try {
            return Integer.parseInt(version);
        } catch (final NumberFormatException e) {
            logger.warn("Failed to determine Java version; Could not parse {} from {}", version, javaVersion, e);
            return -1;
        }
    }

    public static void checkJvm() {
        if (checkJavaVersion() < 11) {
            logger.warn("************************************************************");
            logger.warn("* WARNING - YOU ARE RUNNING AN OUTDATED VERSION OF JAVA.");
            logger.warn("* FASTASYNCWORLDEDIT WILL STOP BEING COMPATIBLE WITH THIS VERSION OF");
            logger.warn("* JAVA WHEN MINECRAFT 1.17 IS RELEASED.");
            logger.warn("*");
            logger.warn("* Please update the version of Java to 11. When Minecraft 1.17");
            logger.warn("* is released, support for versions of Java prior to 11 will");
            logger.warn("* be dropped.");
            logger.warn("*");
            logger.warn("* Current Java version: {}", System.getProperty("java.version"));
            logger.warn("************************************************************");
        }
        if (checkJavaVersion() >= 15) {
            logger.warn("************************************************************");
            logger.warn("* FastAsyncWorldEdit uses Nashorn for the craftscript engine.");
            logger.warn("* Within Java 15, Nashorn has been removed from Java.");
            logger.warn("* Until we add a suitable workaround, you should stick to Java 11");
            logger.warn("* to use all features of FastAsyncWorldEdit.");
            logger.warn("************************************************************");
        }
    }
}
