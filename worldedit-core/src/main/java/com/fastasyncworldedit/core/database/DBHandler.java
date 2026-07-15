package com.fastasyncworldedit.core.database;

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBHandler {

    /**
     * @deprecated Use {@link #dbHandler()} instead.
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static final DBHandler IMP = dbHandler();
    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private final Map<World, RollbackDatabase> databases = new ConcurrentHashMap<>(8, 0.9f, 1);

    /**
     * Get an instance of the DBHandler.
     *
     * @return an instance of the DBHandler.
     * @since 2.0.0
     */
    public static DBHandler dbHandler() {
        return Holder.INSTANCE;
    }

    public RollbackDatabase getDatabase(World world) {
        RollbackDatabase database = databases.get(world);
        if (database != null) {
            return database;
        }
        try {
            RollbackDatabase created = new RollbackDatabase(world);
            RollbackDatabase existing = databases.putIfAbsent(world, created);
            if (existing != null) {
                created.close();
                return existing;
            }
            return created;
        } catch (Throwable e) {
            LOGGER.error("No JDBC driver found!", e);
            return null;
        }
    }

    /**
     * Initialization-on-demand holder: guarantees thread-safe, lazy construction of the
     * {@link DBHandler} singleton without needing explicit synchronization on every access.
     */
    private static final class Holder {

        private static final DBHandler INSTANCE = new DBHandler();

    }

}
