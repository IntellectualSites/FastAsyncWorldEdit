package com.boydti.fawe.bukkit.adapter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.bukkit.listener.ChunkListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BukkitQueueHandler extends QueueHandler {
    private volatile boolean timingsEnabled;
    private static boolean alertTimingsChange = true;

    private static Field fieldTimingsEnabled;
    private static Field fieldAsyncCatcherEnabled;
    private static Method methodCheck;
    static {
        try {
            fieldAsyncCatcherEnabled = Class.forName("org.spigotmc.AsyncCatcher").getField("enabled");
            fieldAsyncCatcherEnabled.setAccessible(true);
        } catch (Throwable ignore) {}
        try {
            fieldTimingsEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");
            fieldTimingsEnabled.setAccessible(true);
            methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
            methodCheck.setAccessible(true);
        } catch (Throwable ignore){}
    }

    public void startSet(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                if (fieldAsyncCatcherEnabled != null) {
                    fieldAsyncCatcherEnabled.set(null, false);
                }
                if (fieldTimingsEnabled != null) {
                    timingsEnabled = (boolean) fieldTimingsEnabled.get(null);
                    if (timingsEnabled) {
                        if (alertTimingsChange) {
                            alertTimingsChange = false;
                            Fawe.debug("Having `parallel-threads` > 1 interferes with the timings.");
                        }
                        fieldTimingsEnabled.set(null, false);
                        methodCheck.invoke(null);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void endSet(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {
                if (fieldAsyncCatcherEnabled != null) {
                    fieldAsyncCatcherEnabled.set(null, true);
                }
                if (fieldTimingsEnabled != null && timingsEnabled) {
                    fieldTimingsEnabled.set(null, true);
                    methodCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IQueueExtent create() {
        return null;
    }
}
