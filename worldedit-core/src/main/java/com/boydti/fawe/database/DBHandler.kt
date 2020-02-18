package com.boydti.fawe.database;

import com.boydti.fawe.config.Config;
import com.sk89q.worldedit.world.World;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBHandler {

    private final Logger log = LoggerFactory.getLogger(Config.class);

    public final static DBHandler IMP = new DBHandler();

    private Map<String, RollbackDatabase> databases = new ConcurrentHashMap<>(8, 0.9f, 1);

    public RollbackDatabase getDatabase(World world) {
        String worldName = world.getName();
        RollbackDatabase database = databases.get(worldName);
        if (database != null) {
            return database;
        }
        try {
            database = new RollbackDatabase(world);
            databases.put(worldName, database);
            return database;
        } catch (Throwable e) {
            log.error("No JDBC driver found!\n TODO: Bundle driver with FAWE (or disable database)", e);
            return null;
        }
    }

    public RollbackDatabase getDatabase(String world) {
        RollbackDatabase database = databases.get(world);
        if (database != null) {
            return database;
        }
        try {
            database = new RollbackDatabase(world);
            databases.put(world, database);
            return database;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
