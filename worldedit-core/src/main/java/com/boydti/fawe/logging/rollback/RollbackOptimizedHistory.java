package com.boydti.fawe.logging.rollback;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class RollbackOptimizedHistory extends DiskStorageHistory {
    private long time;

    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;
    private String command;

    public RollbackOptimizedHistory(World world, UUID uuid, int index) {
        super(world, uuid, index);
        this.time = System.currentTimeMillis();
    }

    public RollbackOptimizedHistory(World world, UUID uuid) {
        super(world, uuid);
        this.time = System.currentTimeMillis();
    }

    public RollbackOptimizedHistory(String world, UUID uuid, int index) {
        super(world, uuid, index);
        this.time = System.currentTimeMillis();
    }

    public RollbackOptimizedHistory(String world, UUID uuid) {
        super(world, uuid);
        this.time = System.currentTimeMillis();
    }

    public RollbackOptimizedHistory(World world, UUID uuid, int index, long time, long size, CuboidRegion region, String command) {
        super(world, uuid, index);
        this.time = time;
        this.minX = region.getMinimumX();
        this.minY = region.getMinimumY();
        this.minZ = region.getMinimumZ();
        this.maxX = region.getMaximumX();
        this.maxY = region.getMaximumY();
        this.maxZ = region.getMaximumZ();
        this.blockSize = (int) size;
        this.command = command;
        this.closed = true;
    }

    public long getTime() {
        return time;
    }

    @Override
    protected DiskStorageSummary summarizeShallow() {
        DiskStorageSummary summary = super.summarizeShallow();
        summary.minX = this.minX;
        summary.minZ = this.minZ;
        summary.maxX = this.maxX;
        summary.maxZ = this.maxZ;
        return summary;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setDimensions(BlockVector3 pos1, BlockVector3 pos2) {
        this.minX = pos1.getBlockX();
        this.minY = pos1.getBlockY();
        this.minZ = pos1.getBlockZ();
        this.maxX = pos2.getBlockX();
        this.maxY = pos2.getBlockY();
        this.maxZ = pos2.getBlockZ();
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public void close() throws IOException {
        super.close();
        // Save to DB
        RollbackDatabase db = DBHandler.IMP.getDatabase(getWorld());
        if (db != null) {
            db.logEdit(this);
        }
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        super.add(x, y, z, combinedFrom, combinedTo);
        if (x < minX) {
            minX = x;
        } else if (x > maxX) {
            maxX = x;
        }
        if (y < minY) {
            minY = y;
        } else if (y > maxY) {
            maxY = y;
        }
        if (z < minZ) {
            minZ = z;
        } else if (z > maxZ) {
            maxZ = z;
        }
    }

    @Override
    public void writeHeader(OutputStream os, int x, int y, int z) throws IOException {
        minX = x;
        maxX = x;
        minY = y;
        maxY = y;
        minZ = z;
        maxZ = z;
        super.writeHeader(os, x, y, z);
    }

    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(minX, minY, minZ);
    }

    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(maxX, maxY, maxZ);
    }
}
