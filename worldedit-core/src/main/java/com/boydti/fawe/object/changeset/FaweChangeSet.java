package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.google.common.util.concurrent.Futures;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FaweChangeSet implements ChangeSet {

    private World world;
    private final String worldName;
    private final boolean mainThread;
    private final int layers;
    protected AtomicInteger waitingCombined = new AtomicInteger(0);
    protected AtomicInteger waitingAsync = new AtomicInteger(0);

    public static FaweChangeSet getDefaultChangeSet(World world, UUID uuid) {
        if (Settings.IMP.HISTORY.USE_DISK) {
            if (Settings.IMP.HISTORY.USE_DATABASE) {
                return new RollbackOptimizedHistory(world, uuid);
            } else {
                return new DiskStorageHistory(world, uuid);
            }
        } else {
            return new MemoryOptimizedHistory(world);
        }
    }

    public FaweChangeSet(String world) {
        this.worldName = world;
        this.mainThread = Fawe.get() == null || Fawe.isMainThread();
        this.layers = FaweCache.IMP.CHUNK_LAYERS;
    }

    public FaweChangeSet(World world) {
        this.world = world;
        this.worldName = world.getName();
        this.mainThread = Fawe.isMainThread();
        this.layers = this.world.getMaxY() + 1 >> 4;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        if (world == null && worldName != null) world = FaweAPI.getWorld(worldName);
        return world;
    }

    @Deprecated
    public boolean flushAsync() {
        return closeAsync();
    }

    public boolean closeAsync() {
        waitingAsync.incrementAndGet();
        TaskManager.IMP.async(() -> {
            waitingAsync.decrementAndGet();
            synchronized (waitingAsync) {
                waitingAsync.notifyAll();
            }
            close();
        });
        return true;
    }

    public boolean flush() {
        try {
            if (!Fawe.isMainThread()) {
                while (waitingAsync.get() > 0) {
                    synchronized (waitingAsync) {
                        waitingAsync.wait(1000);
                    }
                }
            }
            while (waitingCombined.get() > 0) {
                synchronized (waitingCombined) {
                    waitingCombined.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean close() {
        return flush();
    }

    public abstract void add(int x, int y, int z, int combinedFrom, int combinedTo);

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(true);
    }

    public abstract void addTileCreate(CompoundTag tag);

    public abstract void addTileRemove(CompoundTag tag);

    public abstract void addEntityRemove(CompoundTag tag);

    public abstract void addEntityCreate(CompoundTag tag);

    public abstract void addBiomeChange(int x, int z, BiomeType from, BiomeType to);

    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    public abstract Iterator<Change> getIterator(boolean redo);

    public void delete() {
    }

    public EditSession toEditSession(FawePlayer player) {
        return toEditSession(player, null);
    }

    public EditSession toEditSession(FawePlayer player, Region[] regions) {
        EditSessionBuilder builder = new EditSessionBuilder(getWorld()).player(player).autoQueue(false).fastmode(false).checkMemory(false).changeSet(this).limitUnlimited();
        if (regions != null) {
            builder.allowedRegions(regions);
        } else {
            builder.allowedRegionsEverywhere();
        }
        EditSession editSession = builder.build();
        editSession.setSize(1);
        return editSession;
    }

    public void add(EntityCreate change) {
        CompoundTag tag = change.state.getNbtData();
        MainUtil.setEntityInfo(tag, change.getEntity());
        addEntityCreate(tag);
    }

    public void add(EntityRemove change) {
        CompoundTag tag = change.state.getNbtData();
        MainUtil.setEntityInfo(tag, change.getEntity());
        addEntityRemove(tag);
    }

    @Override
    public void add(Change change) {
        if (change.getClass() == BlockChange.class) {
            add((BlockChange) change);
        } else if (change.getClass() == EntityCreate.class) {
            add((EntityCreate) change);
        } else if (change.getClass() == EntityRemove.class) {
            add((EntityRemove) change);
        } else {
            Fawe.debug("Unknown change: " + change.getClass());
        }
    }

    public void add(BlockChange change) {
        try {
            BlockVector3 loc = change.getPosition();
            BaseBlock from = change.getPrevious();
            BaseBlock to = change.getCurrent();
            add(loc, from, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(BlockVector3 loc, BaseBlock from, BaseBlock to) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        add(x, y, z, from, to);
    }

    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        try {
            if (from.hasNbtData()) {
                CompoundTag nbt = from.getNbtData();
                assert nbt != null;
                MainUtil.setPosition(nbt, x, y, z);
                addTileRemove(nbt);
            }
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                assert nbt != null;
                MainUtil.setPosition(nbt, x, y, z);
                addTileCreate(nbt);
            }
            int combinedFrom = from.getInternalId();
            int combinedTo = to.getInternalId();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty() {
        return waitingCombined.get() == 0 && waitingAsync.get() == 0 && size() == 0;
    }

    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        try {
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                assert nbt != null;
                MainUtil.setPosition(nbt, x, y, z);
                addTileCreate(nbt);
            }
            int combinedTo = to.getInternalId();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Future<?> addWriteTask(Runnable writeTask) {
        return addWriteTask(writeTask, Fawe.isMainThread());
    }

    public Future<?> addWriteTask(Runnable writeTask, boolean completeNow) {
        FaweChangeSet.this.waitingCombined.incrementAndGet();
        Runnable wrappedTask = () -> {
            try {
                writeTask.run();
            } finally {
                if (FaweChangeSet.this.waitingCombined.decrementAndGet() <= 0) {
                    synchronized (FaweChangeSet.this.waitingAsync) {
                        FaweChangeSet.this.waitingAsync.notifyAll();
                    }
                    synchronized (FaweChangeSet.this.waitingCombined) {
                        FaweChangeSet.this.waitingCombined.notifyAll();
                    }
                }
            }
        };
        if (completeNow) {
            wrappedTask.run();
            return Futures.immediateCancelledFuture();
        } else {
            return Fawe.get().getQueueHandler().async(wrappedTask);
        }
    }
}
