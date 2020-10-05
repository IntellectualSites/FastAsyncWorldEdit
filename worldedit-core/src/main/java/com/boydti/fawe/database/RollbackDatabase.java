package com.boydti.fawe.database;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.collection.YieldIterable;
import com.boydti.fawe.object.task.AsyncNotifyQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(RollbackDatabase.class);

    private final String prefix;
    private final File dbLocation;
    private final World world;
    private Connection connection;

    @Language("SQLite")
    private String createTable = "CREATE TABLE IF NOT EXISTS `{0}edits` (`player` BLOB(16) NOT NULL,`id` INT NOT NULL, `time` INT NOT NULL,`x1` INT NOT NULL,`x2` INT NOT NULL,`z1` INT NOT NULL,`z2` INT NOT NULL,`y1` INT NOT NULL, `y2` INT NOT NULL, `size` INT NOT NULL, `command` VARCHAR, PRIMARY KEY (player, id))";
    @Language("SQLite")
    private String updateTable1 = "ALTER TABLE `{0}edits` ADD COLUMN `command` VARCHAR";
    @Language("SQLite")
    private String updateTable2 = "alter table `{0}edits` add size int default 0 not null";
    @Language("SQLite")
    private String insertEdit = "INSERT OR REPLACE INTO `{0}edits` (`player`,`id`,`time`,`x1`,`x2`,`z1`,`z2`,`y1`,`y2`,`command`,`size`) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    @Language("SQLite")
    private String purge = "DELETE FROM `{0}edits` WHERE `time`<?";
    @Language("SQLite")
    private String getEditsUser = "SELECT * FROM `{0}edits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND `y2`>=? AND `y1`<=? AND `player`=? ORDER BY `time` DESC, `id` DESC";
    @Language("SQLite")
    private String getEditsUserAsc = "SELECT * FROM `{0}edits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND `y2`>=? AND `y1`<=? AND `player`=? ORDER BY `time` ASC, `id` ASC";
    @Language("SQLite")
    private String getEdits = "SELECT * FROM `{0}edits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND `y2`>=? AND `y1`<=? ORDER BY `time` DESC, `id` DESC";
    @Language("SQLite")
    private String getEditsAsc = "SELECT * FROM `{0}edits` WHERE `time`>? AND `x2`>=? AND `x1`<=? AND `z2`>=? AND `z1`<=? AND `y2`>=? AND `y1`<=? ORDER BY `time` , `id` ";
    @Language("SQLite")
    private String getEditUser = "SELECT * FROM `{0}edits` WHERE `player`=? AND `id`=?";

    @Language("SQLite")
    private String deleteEditsUser = "DELETE FROM `{0}edits` WHERE `player`=? AND `time`>? AND `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=?";
    @Language("SQLite")
    private String deleteEditUser = "DELETE FROM `{0}edits` WHERE `player`=? AND `id`=?";

    private final ConcurrentLinkedQueue<RollbackOptimizedHistory> historyChanges = new ConcurrentLinkedQueue<>();

    RollbackDatabase(World world) throws SQLException, ClassNotFoundException {
        super((t, e) -> e.printStackTrace());
        this.prefix = "";
        this.world = world;
        this.dbLocation = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world.getName() + File.separator + "summary.db");
        connection = openConnection();

        // update vars
        createTable = createTable.replace("{0}", prefix);
        updateTable1 = updateTable1.replace("{0}", prefix);
        updateTable2 = updateTable2.replace("{0}", prefix);
        insertEdit = insertEdit.replace("{0}", prefix);
        purge = purge.replace("{0}", prefix);
        getEditsUser = getEditsUser.replace("{0}", prefix);
        getEditsUserAsc = getEditsUserAsc.replace("{0}", prefix);
        getEdits = getEdits.replace("{0}", prefix);
        getEditsAsc = getEditsAsc.replace("{0}", prefix);
        getEditUser = getEditUser.replace("{0}", prefix);
        deleteEditsUser = deleteEditsUser.replace("{0}", prefix);
        deleteEditUser = deleteEditUser.replace("{0}", prefix);

        try {
            init().get();
            purge((int) TimeUnit.DAYS.toSeconds(Settings.IMP.HISTORY.DELETE_AFTER_DAYS));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    public  Future<Boolean> init() {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(updateTable1)) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
            } // Already updated
            try (PreparedStatement stmt = connection.prepareStatement(updateTable2)) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
            } // Already updated
            return true;
        });
    }

    public Future<Integer> delete(UUID uuid, int id) {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(deleteEditUser)) {
                stmt.setBytes(1, toBytes(uuid));
                stmt.setInt(2, id);
                return stmt.executeUpdate();
            }
        });
    }

    public Future<RollbackOptimizedHistory> getEdit(@NotNull UUID uuid, int id) {
        return call(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(getEditUser)) {
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
        int y1 = result.getByte("y1") + 128;
        int y2 = result.getByte("y2") + 128;
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
            try (PreparedStatement stmt = connection.prepareStatement(purge)) {
                stmt.setInt(1, then);
                return stmt.executeUpdate();
            }
        });
    }

    public Iterable<Supplier<RollbackOptimizedHistory>> getEdits(BlockVector3 pos, boolean ascending) {
        return getEdits(null, 0, pos, pos, false, ascending);
    }

    public Iterable<Supplier<RollbackOptimizedHistory>> getEdits(UUID uuid, long minTime, BlockVector3 pos1, BlockVector3 pos2, boolean delete, boolean ascending) {
        YieldIterable<Supplier<RollbackOptimizedHistory>> yieldIterable = new YieldIterable<>();

        Future<Integer> future = call(() -> {
            try {
                int count = 0;
                String stmtStr = ascending ? uuid == null ? getEditsAsc : getEditsUserAsc :
                        uuid == null ? getEdits : getEditsUser;
                try (PreparedStatement stmt = connection.prepareStatement(stmtStr)) {
                    stmt.setInt(1, (int) (minTime / 1000));
                    stmt.setInt(2, pos1.getBlockX());
                    stmt.setInt(3, pos2.getBlockX());
                    stmt.setInt(4, pos1.getBlockZ());
                    stmt.setInt(5, pos2.getBlockZ());
                    stmt.setByte(6, (byte) (pos1.getBlockY() - 128));
                    stmt.setByte(7, (byte) (pos2.getBlockY() - 128));
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
                    try (PreparedStatement stmt = connection.prepareStatement(deleteEditsUser)) {
                        stmt.setInt(1, (int) (minTime / 1000));
                        stmt.setInt(2, pos1.getBlockX());
                        stmt.setInt(3, pos2.getBlockX());
                        stmt.setInt(4, pos1.getBlockZ());
                        stmt.setInt(5, pos2.getBlockZ());
                        stmt.setByte(6, (byte) (pos1.getBlockY() - 128));
                        stmt.setByte(7, (byte) (pos2.getBlockY() - 128));
                        byte[] uuidBytes = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
                        stmt.setBytes(8, uuidBytes);
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

        try (PreparedStatement stmt = connection.prepareStatement(insertEdit)) {
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
                stmt.setByte(8, (byte) (pos1.getY() - 128));
                stmt.setByte(9, (byte) (pos2.getY() - 128));
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
        if (!Fawe.imp().getDirectory().exists()) {
            Fawe.imp().getDirectory().mkdirs();
        }
        if (!dbLocation.exists()) {
            try {
                dbLocation.getParentFile().mkdirs();
                dbLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                log.debug("Unable to create the database!");
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
