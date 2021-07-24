package com.fastasyncworldedit.bukkit.adapter;

import co.aikar.timings.Timings;
import com.fastasyncworldedit.bukkit.listener.ChunkListener;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.spigotmc.AsyncCatcher;

import java.lang.reflect.Method;

public class BukkitQueueHandler extends QueueHandler {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private volatile boolean timingsEnabled;
    private static boolean alertTimingsChange = true;

    private static Method methodCheck;

    static {
        try {
            methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
            methodCheck.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void startSet(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                AsyncCatcher.enabled = false;
                timingsEnabled = Timings.isTimingsEnabled();
                if (timingsEnabled) {
                    if (alertTimingsChange) {
                        alertTimingsChange = false;
                        LOGGER.debug("Having `parallel-threads` > 1 interferes with the timings.");
                    }
                    Timings.setTimingsEnabled(false);
                    methodCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void endSet(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {
                AsyncCatcher.enabled = true;
                if (timingsEnabled) {
                    Timings.setTimingsEnabled(true);
                    methodCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
