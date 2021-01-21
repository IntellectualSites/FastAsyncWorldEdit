package com.boydti.fawe.bukkit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaneSoftwareCheck {

    private static final Logger log = LoggerFactory.getLogger(SaneSoftwareCheck.class);

    public static Class<?> checkVersion() {
        try {
            Class.forName("org.yatopiamc.yatopia.server.YatopiaConfig");
        } catch (ClassNotFoundException e) {
            return null;
        } log.warn("You are running a server fork that is known to be extremely dangerous and lead to data loss. It is strongly recommended you switch to a more stable, high-performing server software, like Paper.");
        return null;
    }
}
