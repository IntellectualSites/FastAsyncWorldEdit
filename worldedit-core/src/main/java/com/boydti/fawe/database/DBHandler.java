package com.boydti.fawe.database;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.world.World;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBHandler {
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
            Fawe.debug("============ NO JDBC DRIVER! ============");
            Fawe.debug("TODO: Bundle driver with FAWE (or disable database)");
            Fawe.debug("=========================================");
            e.printStackTrace();
            Fawe.debug("=========================================");
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
