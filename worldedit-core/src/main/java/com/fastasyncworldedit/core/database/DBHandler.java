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
        // computeIfAbsent (not the earlier get-then-putIfAbsent) serializes construction itself:
        // ConcurrentHashMap guarantees the mapping function runs at most once per key while
        // racing callers for the same world block on it, so at most one RollbackDatabase (and
        // one underlying SQLite connection) is ever constructed per world - putIfAbsent alone
        // still let two threads each open a connection before one was discarded as the loser.
        try {
            return databases.computeIfAbsent(world, w -> {
                try {
                    return new RollbackDatabase(w);
                } catch (Throwable e) {
                    // computeIfAbsent's mapping function can't declare checked exceptions, and no
                    // value is stored if it throws - matching the previous behavior of not
                    // caching a construction failure, so a later call can retry.
                    throw new RollbackDatabaseConstructionException(e);
                }
            });
        } catch (RollbackDatabaseConstructionException e) {
            LOGGER.error("No JDBC driver found!", e.getCause());
            return null;
        }
    }

    private static final class RollbackDatabaseConstructionException extends RuntimeException {

        RollbackDatabaseConstructionException(Throwable cause) {
            super(cause);
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
