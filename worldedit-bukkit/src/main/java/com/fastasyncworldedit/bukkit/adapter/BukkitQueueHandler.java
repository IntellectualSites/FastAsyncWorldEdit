package com.fastasyncworldedit.bukkit.adapter;

import co.aikar.timings.Timings;
import com.fastasyncworldedit.bukkit.listener.ChunkListener;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BukkitQueueHandler extends QueueHandler {

    private volatile boolean timingsEnabled;
    private static boolean alertTimingsChange = true;

    private static Method timingsCheck;
    private static Field asyncCatcher;

    static {
        try {
            timingsCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
            timingsCheck.setAccessible(true);
        } catch (Throwable ignored) {
        }
        try {
            asyncCatcher = Class.forName("org.spigotmc.AsyncCatcher").getDeclaredField("enabled");
            asyncCatcher.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void startUnsafe(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                asyncCatcher.setBoolean(asyncCatcher, false);
                timingsEnabled = Timings.isTimingsEnabled();
                if (timingsEnabled) {
                    if (alertTimingsChange) {
                        alertTimingsChange = false;
                    }
                    Timings.setTimingsEnabled(false);
                    timingsCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void endUnsafe(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {
                asyncCatcher.setBoolean(asyncCatcher, true);
                if (timingsEnabled) {
                    Timings.setTimingsEnabled(true);
                    timingsCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
