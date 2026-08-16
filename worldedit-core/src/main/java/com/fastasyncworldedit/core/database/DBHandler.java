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
                } catch (Exception e) {
                    // Exception, not Throwable: RollbackDatabase's construction only needs
                    // checked/unchecked failures translated into the retryable null result below.
                    // A real Error (e.g. OutOfMemoryError) must still propagate rather than being
                    // logged and swallowed as if it were an ordinary construction failure.
                    //
                    // computeIfAbsent's mapping function can't declare checked exceptions, and no
                    // value is stored if it throws - matching the previous behavior of not
                    // caching a construction failure, so a later call can retry.
                    throw new RollbackDatabaseConstructionException(e);
                }
            });
        } catch (RollbackDatabaseConstructionException e) {
            LOGGER.error("Failed to construct RollbackDatabase for world '{}'", world.getName(), e.getCause());
            return null;
        }
    }

    private static final class RollbackDatabaseConstructionException extends RuntimeException {

        RollbackDatabaseConstructionException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * Initialization-on-demand holder: guarantees thread-safe construction of the
     * {@link DBHandler} singleton without needing explicit synchronization on every access. In
     * isolation this class would also be genuinely <em>lazy</em> - {@code Holder} only loads (and
     * so only constructs {@code INSTANCE}) on first reference from {@link #dbHandler()} - but the
     * deprecated {@link #IMP} field's initializer calls {@code dbHandler()} during
     * {@code DBHandler}'s own static initialization, so in practice {@code INSTANCE} is
     * constructed eagerly, the first time anything touches {@code DBHandler} at all.
     */
    private static final class Holder {

        private static final DBHandler INSTANCE = new DBHandler();

    }

}
