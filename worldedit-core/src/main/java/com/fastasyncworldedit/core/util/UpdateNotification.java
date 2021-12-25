package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import org.apache.logging.log4j.Logger;


public class UpdateNotification {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    /**
     * Check whether a new build with a higher build number than the current build is available.
     */
    public static void doUpdateCheck() {
        if (Settings.IMP.ENABLED_COMPONENTS.UPDATE_NOTIFICATIONS) {
            LOGGER.warn("An update for FastAsyncWorldEdit is available. Update at https://ci.athion.net/job/FastAsyncWorldEdit/");
        }
    }

    /**
     * Trigger an update notification based on captions. Useful to notify server administrators ingame.
     *
     * @param actor The player to notify.
     */
    public static void doUpdateNotification(Actor actor) {
        if (Settings.IMP.ENABLED_COMPONENTS.UPDATE_NOTIFICATIONS) {
            if (actor.hasPermission("fawe.admin")) {
                actor.printInfo(TextComponent.of("An update for FastAsyncWorldEdit is available. Update at " +
                        "https://ci.athion.net/job/FastAsyncWorldEdit/").clickEvent(ClickEvent.openUrl("https://ci.athion.net/job/FastAsyncWorldEdit/"))

                );
            }
        }
    }

}
