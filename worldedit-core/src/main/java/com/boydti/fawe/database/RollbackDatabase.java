package com.boydti.fawe.database;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.task.AsyncNotifyQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
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
import java.util.concurrent.TimeUnit;

public class RollbackDatabase extends AsyncNotifyQueue {

    private final String prefix;
    private final File dbLocation;
    private final String worldName;
    private final World world;
    private Connection connection;

    private String INSERT_EDIT;
    private String CREATE_TABLE;
    //    private String GET_EDITS_POINT;
    private String GET_EDITS;
    private String GET_EDITS_USER;
    private String GET_EDITS_ASC;
    private String GET_EDITS_USER_ASC;
    private String DELETE_EDITS_USER;
    private String DELETE_EDIT_USER;
    private String PURGE;

    private ConcurrentLinkedQueue<RollbackOptimizedHistory> historyChanges = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public RollbackDatabase(String world) throws SQLException, ClassNotFoundException {
        this(FaweAPI.getWorld(world));
    }

    public RollbackDatabase(final World world) throws SQLException, ClassNotFoundException {
        this.prefix = "";
        this.worldName = Fawe.imp().getWorldName(world);
        this.world = world;
        this.dbLocation = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + Fawe.imp().getWorldName(world) + File.separator + "summary.db");
        connection = openConnection();
        CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `" + prefix + "edits` (`player` BLOB(16) NOT NULL,`id` INT NOT NULL,`x1` INT NOT NULL,`y1` INT NOT NULL,`z1` INT NOT NULL,`x2` INT NOT NULL,`y2` INT NOT NULL,`z2` INT NOT NULL,`time` INT NOT NULL, PRIMARY KEY (player, id))";
        INSERT_EDIT = "INSERT OR REPLACE INTO `" + prefix + "edits` (`player`,`id`,`x1`,`y1`,`z1`,`x2`,`y2`,`z2`,`time`) VALUES(?,?,?,?,?,?,?,?,?)";
        PURGE = "DELETE FROM `" + prefix + "edits` WHERE `time`<?";
//        GET_EDITS_POINT = "SELECT `player`,`id` FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=?";
        GET_EDITS = "SELECT `player`,`id` FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=? AND `time`>? ORDER BY `time` DESC, `id` DESC";
        GET_EDITS_USER = "SELECT `player`,`id` FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=? AND `time`>? AND `player`=? ORDER BY `time` DESC, `id` DESC";
        GET_EDITS_ASC = "SELECT `player`,`id` FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=? AND `time`>? ORDER BY `time` ASC, `id` ASC";
        GET_EDITS_USER_ASC = "SELECT `player`,`id` FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=? AND `time`>? AND `player`=? ORDER BY `time` ASC, `id` ASC";
        DELETE_EDITS_USER = "DELETE FROM `" + prefix + "edits` WHERE `x2`>=? AND `x1`<=? AND `y2`>=? AND `y1`<=? AND `z2`>=? AND `z1`<=? AND `time`>? AND `player`=?";
        DELETE_EDIT_USER = "DELETE FROM `" + prefix + "edits` WHERE `player`=? AND `id`=?";
        init();
        purge((int) TimeUnit.DAYS.toMillis(Settings.IMP.HISTORY.DELETE_AFTER_DAYS));
    }


    @Override
    public boolean hasQueued() {
        return connection != null && (!historyChanges.isEmpty() || !tasks.isEmpty());
    }

    @Override
    public void operate() {
        synchronized (this) {
            if (connection == null) {
                return;
            }
            while (sendBatch()) {
                // Still processing
            }
        }
    }

    public void init() {
        try (PreparedStatement stmt = connection.prepareStatement(CREATE_TABLE)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(final UUID uuid, final int id) {
        addTask(new Runnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement(DELETE_EDIT_USER)) {
                    byte[] uuidBytes = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
                    stmt.setBytes(1, uuidBytes);
                    stmt.setInt(2, id);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void purge(int diff) {
        long now = System.currentTimeMillis() / 1000;
        final int then = (int) (now - diff);
        addTask(new Runnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement(PURGE)) {
                    stmt.setInt(1, then);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getPotentialEdits(final UUID uuid, final long minTime, final BlockVector3 pos1, final BlockVector3 pos2, final RunnableVal<DiskStorageHistory> onEach, final Runnable whenDone, final boolean delete, final boolean ascending) {
        final World world = FaweAPI.getWorld(this.worldName);
        addTask(new Runnable() {
            @Override
            public void run() {
                String stmtStr = ascending ? (uuid == null ? GET_EDITS_ASC : GET_EDITS_USER_ASC) : (uuid == null ? GET_EDITS : GET_EDITS_USER);
                try (PreparedStatement stmt = connection.prepareStatement(stmtStr)) {
                    stmt.setInt(1, pos1.getBlockX());
                    stmt.setInt(2, pos2.getBlockX());
                    stmt.setByte(3, (byte) (pos1.getBlockY() - 128));
                    stmt.setByte(4, (byte) (pos2.getBlockY() - 128));
                    stmt.setInt(5, pos1.getBlockZ());
                    stmt.setInt(6, pos2.getBlockZ());
                    stmt.setInt(7, (int) (minTime / 1000));
                    if (uuid != null) {
                        byte[] uuidBytes = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
                        stmt.setBytes(8, uuidBytes);
                    }
                    ResultSet result = stmt.executeQuery();
                    if (!result.next()) {
                        TaskManager.IMP.taskNow(whenDone, false);
                        return;
                    }
                    do {
                        byte[] uuidBytes = result.getBytes(1);
                        int index = result.getInt(2);
                        ByteBuffer bb = ByteBuffer.wrap(uuidBytes);
                        long high = bb.getLong();
                        long low = bb.getLong();
                        DiskStorageHistory history = new DiskStorageHistory(world, new UUID(high, low), index);
                        if (history.getBDFile().exists()) {
                            onEach.run(history);
                        }
                    } while (result.next());
                    TaskManager.IMP.taskNow(whenDone, false);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (delete && uuid != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(DELETE_EDITS_USER)) {
                        stmt.setInt(1, pos1.getBlockX());
                        stmt.setInt(2, pos2.getBlockX());
                        stmt.setByte(3, (byte) (pos1.getBlockY() - 128));
                        stmt.setByte(4, (byte) (pos2.getBlockY() - 128));
                        stmt.setInt(5, pos1.getBlockZ());
                        stmt.setInt(6, pos2.getBlockZ());
                        stmt.setInt(7, (int) (minTime / 1000));
                        byte[] uuidBytes = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
                        stmt.setBytes(8, uuidBytes);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void logEdit(RollbackOptimizedHistory history) {
        queue(() -> historyChanges.add(history));
    }

    public void addTask(Runnable run) {
        queue(() -> tasks.add(run));
    }

    private void runTasks() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean sendBatch() {
        try {
            runTasks();
            commit();
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
            int size = Math.min(1048572, historyChanges.size());

            if (size == 0) {
                return false;
            }

            RollbackOptimizedHistory[] copy = new RollbackOptimizedHistory[size];
            for (int i = 0; i < size; i++) {
                copy[i] = historyChanges.poll();
            }

            try (PreparedStatement stmt = connection.prepareStatement(INSERT_EDIT)) {
                for (RollbackOptimizedHistory change : copy) {
                    // `player`,`id`,`x1`,`y1`,`z1`,`x2`,`y2`,`z2`,`time`
                    UUID uuid = change.getUUID();
                    byte[] uuidBytes = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
                    stmt.setBytes(1, uuidBytes);
                    stmt.setInt(2, change.getIndex());
                    stmt.setInt(3, change.getMinX());
                    stmt.setByte(4, (byte) (change.getMinY() - 128));
                    stmt.setInt(5, change.getMinZ());
                    stmt.setInt(6, change.getMaxX());
                    stmt.setByte(7, (byte) (change.getMaxY() - 128));
                    stmt.setInt(8, change.getMaxZ());
                    stmt.setInt(9, (int) (change.getTime() / 1000));
                    stmt.executeUpdate();
                    stmt.clearParameters();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            commit();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void commit() {
        try {
            if (connection == null) {
                return;
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection openConnection() throws SQLException, ClassNotFoundException {
        if (checkConnection()) {
            return connection;
        }
        if (!Fawe.imp().getDirectory().exists()) {
            Fawe.imp().getDirectory().mkdirs();
        }
        if (!(dbLocation.exists())) {
            try {
                dbLocation.getParentFile().mkdirs();
                dbLocation.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
                Fawe.debug("&cUnable to create database!");
            }
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        return connection;
    }

    public Connection forceConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        return connection;
    }

    /**
     * Gets the connection with the database
     *
     * @return Connection with the database, null if none
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                forceConnection();
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    /**
     * Closes the connection with the database
     *
     * @return true if successful
     * @throws java.sql.SQLException if the connection cannot be closed
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
     * Checks if a connection is open with the database
     *
     * @return true if the connection is open
     * @throws java.sql.SQLException if the connection cannot be checked
     */
    public boolean checkConnection() {
        try {
            return (connection != null) && !connection.isClosed();
        } catch (final SQLException e) {
            return false;
        }
    }
}
