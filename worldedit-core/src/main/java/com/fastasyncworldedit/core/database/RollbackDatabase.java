package com.fastasyncworldedit.core.database;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.RollbackOptimizedHistory;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.collection.YieldIterable;
import com.fastasyncworldedit.core.util.task.AsyncNotifyQueue;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class RollbackDatabase extends AsyncNotifyQueue {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final String prefix;
    private final File dbLocation;
    private final World world;
    private final ConcurrentLinkedQueue<RollbackOptimizedHistory> historyChanges = new ConcurrentLinkedQueue<>();
    private Connection connection;

    RollbackDatabase(World world) throws SQLException, ClassNotFoundException {
        super((t, e) -> e.printStackTrace());
        this.prefix = "";
        this.world = world;
        this.dbLocation = MainUtil.getFile(
                Fawe.platform().getDirectory(),
                Settings.settings().PATHS.HISTORY + File.separator + world.getName() + File.separator + "summary.db"
        );
        connection = openConnection();

        try {
            init().get();
            purge((int) TimeUnit.DAYS.toSeconds(Settings.settings().HISTORY.DELETE_AFTER_DAYS));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    public Future<Boolean> init() {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("CREATE TABLE IF NOT EXISTS`" + this.prefix +
                    "edits` (`player` BLOB(16) NOT NULL,`id` INT NOT NULL, `time` INT NOT NULL,`x1`" +
                    "INT NOT NULL,`x2` INT NOT NULL,`z1` INT NOT NULL,`z2` INT NOT NULL,`y1`" +
                    "INT NOT NULL, `y2` INT NOT NULL, `size` INT NOT NULL, `command` VARCHAR, PRIMARY KEY (player, id))")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement("ALTER TABLE`" + this.prefix + "edits` ADD COLUMN `command` VARCHAR")) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
            } // Already updated
            try (PreparedStatement stmt = connection.prepareStatement("ALTER TABLE`" + this.prefix + "edits` ADD SIZE INT DEFAULT 0 NOT NULL")) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
            } // Already updated
            return true;
        });
    }

    public Future<Integer> delete(UUID uuid, int id) {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM`" + this.prefix + "edits` WHERE `player`=? AND `id`=?")) {
                stmt.setBytes(1, toBytes(uuid));
                stmt.setInt(2, id);
                return stmt.executeUpdate();
            }
        });
    }

    public Future<RollbackOptimizedHistory> getEdit(@Nonnull UUID uuid, int id) {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM`" + this.prefix +
                    "edits` WHERE `player`=? AND `id`=?")) {
                stmt.setBytes(1, toBytes(uuid));
                stmt.setInt(2, id);
                ResultSet result = stmt.executeQuery();
                if (!result.next()) {
                    return null;
                }
                return create(result).get();
            }
        });
    }

    private Supplier<RollbackOptimizedHistory> create(ResultSet result) throws SQLException {
        byte[] uuidBytes = result.getBytes("player");
        int index = result.getInt("id");
        int x1 = result.getInt("x1");
        int x2 = result.getInt("x2");
        // Keep 128 offset for backwards-compatibility
        int y1 = result.getInt("y1") + 128;
        int y2 = result.getInt("y2") + 128;
        int z1 = result.getInt("z1");
        int z2 = result.getInt("z2");
        CuboidRegion region = new CuboidRegion(BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));

        long time = result.getInt("time") * 1000L;
        long size = result.getInt("size");

        String command = result.getString("command");

        ByteBuffer bb = ByteBuffer.wrap(uuidBytes);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(high, low);

        return () -> new RollbackOptimizedHistory(world, uuid, index, time, size, region, command);
    }

    public Future<Integer> purge(int diff) {
        long now = System.currentTimeMillis() / 1000;
        final int then = (int) (now - diff);
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM`" + this.prefix + "edits` WHERE `time`<?")) {
                stmt.setInt(1, then);
                return stmt.executeUpdate();
            }
        });
    }

    public Iterable<Supplier<RollbackOptimizedHistory>> getEdits(BlockVector3 pos, boolean ascending) {
        return getEdits(null, 0, pos, pos, false, ascending);
    }

    public Iterable<Supplier<RollbackOptimizedHistory>> getEdits(
            UUID uuid,
            long minTime,
            BlockVector3 pos1,
            BlockVector3 pos2,
            boolean delete,
            boolean ascending
    ) {
        YieldIterable<Supplier<RollbackOptimizedHistory>> yieldIterable = new YieldIterable<>();

        Future<Integer> future = call(() -> {
            try {
                int count = 0;
                String stmtStr;
                if (ascending) {
                    if (uuid == null) {
                        stmtStr = "SELECT * FROM`%sedits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND " +
                                "`y2`>=? AND `y1`<=? ORDER BY `time` , `id`";
                    } else {
                        stmtStr = "SELECT * FROM`%sedits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND " +
                                "`y2`>=? AND `y1`<=? AND `player`=? ORDER BY `time` ASC, `id` ASC";
                    }
                } else {
                    stmtStr = "SELECT * FROM`%sedits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND " +
                            "`y2`>=? AND `y1`<=? AND `player`=? ORDER BY `time` DESC, `id` DESC";
                }
                try (PreparedStatement stmt = connection.prepareStatement(stmtStr.formatted(this.prefix))) {
                    stmt.setInt(1, (int) (minTime / 1000));
                    stmt.setInt(2, pos1.getBlockX());
                    stmt.setInt(3, pos2.getBlockX());
                    stmt.setInt(4, pos1.getBlockZ());
                    stmt.setInt(5, pos2.getBlockZ());
                    // Keep 128 offset for backwards-compatibility
                    stmt.setInt(6, pos1.getBlockY() - 128);
                    stmt.setInt(7, pos2.getBlockY() - 128);
                    if (uuid != null) {
                        byte[] uuidBytes = toBytes(uuid);
                        stmt.setBytes(8, uuidBytes);
                    }
                    ResultSet result = stmt.executeQuery();
                    if (!result.next()) {
                        return 0;
                    }
                    do {
                        count++;
                        Supplier<RollbackOptimizedHistory> history = create(result);
                        yieldIterable.accept(history);
                    } while (result.next());
                }
                if (delete && uuid != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM`" + this.prefix +
                            "edits` WHERE `player`=? AND `time`>? AND `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=?")) {
                        byte[] uuidBytes = ByteBuffer
                                .allocate(16)
                                .putLong(uuid.getMostSignificantBits())
                                .putLong(uuid.getLeastSignificantBits())
                                .array();
                        stmt.setBytes(1, uuidBytes);
                        stmt.setInt(2, (int) (minTime / 1000));
                        stmt.setInt(3, pos1.getBlockX());
                        stmt.setInt(4, pos2.getBlockX());
                        stmt.setInt(5, pos1.getBlockZ());
                        stmt.setInt(6, pos2.getBlockZ());
                        // Keep 128 offset for backwards-compatibility
                        stmt.setInt(7, pos1.getBlockY() - 128);
                        stmt.setInt(8, pos2.getBlockY() - 128);
                    }
                }
                return count;
            } finally {
                yieldIterable.close();
            }
        });
        yieldIterable.setFuture(future);

        return yieldIterable;
    }

    public Future<?> logEdit(RollbackOptimizedHistory history) {
        historyChanges.add(history);
        return call(this::sendBatch);
    }

    private boolean sendBatch() throws SQLException {
        int size = Math.min(1048572, historyChanges.size());

        if (size == 0) {
            return false;
        }

        commit();
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }

        RollbackOptimizedHistory[] copy = IntStream.range(0, size)
                .mapToObj(i -> historyChanges.poll()).toArray(RollbackOptimizedHistory[]::new);

        try (PreparedStatement stmt = connection.prepareStatement("INSERT OR REPLACE INTO`" + this.prefix + "edits`" +
                " (`player`,`id`,`time`,`x1`,`x2`,`z1`,`z2`,`y1`,`y2`,`command`,`size`) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            // `player`,`id`,`time`,`x1`,`x2`,`z1`,`z2`,`y1`,`y2`,`command`,`size`) VALUES(?,?,?,?,?,?,?,?,?,?,?)"
            for (RollbackOptimizedHistory change : copy) {
                UUID uuid = change.getUUID();
                byte[] uuidBytes = toBytes(uuid);
                stmt.setBytes(1, uuidBytes);
                stmt.setInt(2, change.getIndex());
                stmt.setInt(3, (int) (change.getTime() / 1000));

                BlockVector3 pos1 = change.getMinimumPoint();
                BlockVector3 pos2 = change.getMaximumPoint();

                stmt.setInt(4, pos1.getX());
                stmt.setInt(5, pos2.getX());
                stmt.setInt(6, pos1.getZ());
                stmt.setInt(7, pos2.getZ());
                // Keep 128 offset for backwards-compatibility
                stmt.setInt(8, pos1.getY() - 128);
                stmt.setInt(9, pos2.getY() - 128);
                stmt.setString(10, change.getCommand());
                stmt.setInt(11, change.size());
                stmt.executeUpdate();
                stmt.clearParameters();
            }
        } finally {
            commit();
        }
        return true;
    }

    private void commit() {
        try {
            if (connection == null) {
                return;
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection openConnection() throws SQLException, ClassNotFoundException {
        if (checkConnection()) {
            return connection;
        }
        if (!Fawe.platform().getDirectory().exists()) {
            Fawe.platform().getDirectory().mkdirs();
        }
        if (!dbLocation.exists()) {
            try {
                dbLocation.getParentFile().mkdirs();
                dbLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Unable to create the database!");
            }
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        return connection;
    }

    private Connection forceConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        return connection;
    }

    /**
     * Gets the connection with the database.
     *
     * @return Connection with the database, null if none
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                forceConnection();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    /**
     * Closes the connection with the database.
     *
     * @return true if successful
     * @throws SQLException if the connection cannot be closed
     */
    public boolean closeConnection() throws SQLException {
        if (connection == null) {
            return false;
        }
        synchronized (this) {
            if (connection == null) {
                return false;
            }
            connection.close();
            connection = null;
            return true;
        }
    }

    /**
     * Checks if a connection is open with the database.
     *
     * @return true if the connection is open
     */
    public boolean checkConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.close();
    }

}
