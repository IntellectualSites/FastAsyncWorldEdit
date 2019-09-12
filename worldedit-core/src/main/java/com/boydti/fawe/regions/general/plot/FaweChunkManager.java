package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.github.intellectualsites.plotsquared.plot.object.ChunkLoc;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.util.ChunkManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

import java.util.concurrent.CompletableFuture;

public class FaweChunkManager extends ChunkManager {

    private ChunkManager parent;

    public FaweChunkManager(ChunkManager parent) {
        this.parent = parent;
    }

    @Override
    public int[] countEntities(Plot plot) {
        return parent.countEntities(plot);
    }

    @Override
    public CompletableFuture loadChunk(String world, ChunkLoc loc, boolean force) {
        return parent.loadChunk(world, loc, force);
    }

    @Override
    public void unloadChunk(String s, ChunkLoc chunkLoc, boolean save) {
        parent.unloadChunk(s, chunkLoc, save);
    }

    @Override
    public void clearAllEntities(Location pos1, Location pos2) {
        parent.clearAllEntities(pos1, pos2);
    }

    @Override
    public void swap(final Location pos1, final Location pos2, final Location pos3, final Location pos4, final Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweChunkManager.class) {
                EditSession sessionA = new EditSessionBuilder(pos1.getWorld()).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                EditSession sessionB = new EditSessionBuilder(pos3.getWorld()).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                CuboidRegion regionA = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
                CuboidRegion regionB = new CuboidRegion(BlockVector3.at(pos3.getX(), pos3.getY(), pos3.getZ()), BlockVector3.at(pos4.getX(), pos4.getY(), pos4.getZ()));
                ForwardExtentCopy copyA = new ForwardExtentCopy(sessionA, regionA, sessionB, regionB.getMinimumPoint());
                ForwardExtentCopy copyB = new ForwardExtentCopy(sessionB, regionB, sessionA, regionA.getMinimumPoint());
                try {
                    Operations.completeLegacy(copyA);
                    Operations.completeLegacy(copyB);
                    sessionA.flushQueue();
                    sessionB.flushQueue();
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
                TaskManager.IMP.task(whenDone);
            }
        });
    }

    @Override
    public boolean copyRegion(final Location pos1, final Location pos2, final Location pos3, final Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweChunkManager.class) {
                EditSession from = new EditSessionBuilder(pos1.getWorld()).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                EditSession to = new EditSessionBuilder(pos3.getWorld()).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
                ForwardExtentCopy copy = new ForwardExtentCopy(from, region, to, BlockVector3.at(pos3.getX(), pos3.getY(), pos3.getZ()));
                try {
                    Operations.completeLegacy(copy);
                    to.flushQueue();
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
            }
            TaskManager.IMP.task(whenDone);
        });
        return true;
    }

    @Override
    public boolean regenerateRegion(final Location pos1, final Location pos2, boolean ignore, final Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweChunkManager.class) {
                EditSession editSession = new EditSessionBuilder(pos1.getWorld()).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                World world = editSession.getWorld();
                CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
                world.regenerate(region, editSession);
                editSession.flushQueue();
                TaskManager.IMP.task(whenDone);
            }
        });
        return true;
    }
}
