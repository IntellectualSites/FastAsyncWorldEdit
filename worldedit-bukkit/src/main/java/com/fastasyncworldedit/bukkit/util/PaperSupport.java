package com.fastasyncworldedit.bukkit.util;

import org.jetbrains.annotations.ApiStatus;

/**
 * Not for external use. May change at any point.
 */
@ApiStatus.Internal
public enum PaperSupport {
    ;
    public static final boolean PAPER;

    static {
        PAPER = hasClass("com.destroystokyo.paper.PaperConfig")
            || hasClass("io.papermc.paper.configuration.Configuration");
    }

    /**
     * Whether the server is Paper or a fork of it. FAWE refuses to load otherwise.
     */
    public static boolean isPaper() {
        return PAPER;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
