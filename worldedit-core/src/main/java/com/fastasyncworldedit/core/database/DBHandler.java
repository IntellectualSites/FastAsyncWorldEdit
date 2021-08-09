package com.fastasyncworldedit.core.database;

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBHandler {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public static final DBHandler IMP = new DBHandler();

    private final Map<World, RollbackDatabase> databases = new ConcurrentHashMap<>(8, 0.9f, 1);

    public RollbackDatabase getDatabase(World world) {
        RollbackDatabase database = databases.get(world);
        if (database != null) {
            return database;
        }
        try {
            database = new RollbackDatabase(world);
            databases.put(world, database);
            return database;
        } catch (Throwable e) {
            LOGGER.error("No JDBC driver found!", e);
            return null;
        }
    }

}
