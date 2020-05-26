package com.boydti.fawe.bukkit.adapter;

import static org.slf4j.LoggerFactory.getLogger;

import co.aikar.timings.Timings;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.bukkit.listener.ChunkListener;
import java.lang.reflect.Method;
import org.spigotmc.AsyncCatcher;

public class BukkitQueueHandler extends QueueHandler {
    private volatile boolean timingsEnabled;
    private static boolean alertTimingsChange = true;

    private static Method methodCheck;
    static {
        try {
            methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
            methodCheck.setAccessible(true);
        } catch (Throwable ignore){}
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
                        getLogger(BukkitQueueHandler.class).debug("Having `parallel-threads` > 1 interferes with the timings.");
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
