package com.boydti.fawe.object.changeset;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.google.common.util.concurrent.Futures;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
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
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FaweChangeSet implements ChangeSet, IBatchProcessor, Closeable {

    private World world;
    private final String worldName;
    protected AtomicInteger waitingCombined = new AtomicInteger(0);
    protected AtomicInteger waitingAsync = new AtomicInteger(0);
    private boolean closed;

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
    }

    public FaweChangeSet(World world) {
        this.world = world;
        this.worldName = world.getName();
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        if (world == null && worldName != null) world = FaweAPI.getWorld(worldName);
        return world;
    }

    public void closeAsync() {
        if (closed) return;
        closed = true;
        waitingAsync.incrementAndGet();
        TaskManager.IMP.async(() -> {
            waitingAsync.decrementAndGet();
            synchronized (waitingAsync) {
                waitingAsync.notifyAll();
            }
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void flush() {
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
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            flush();
        }
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

    @Override
    public Extent construct(Extent child) {
        return new HistoryExtent(child, this);
    }

    @Override
    public synchronized IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;

        Map<BlockVector3, CompoundTag> tilesFrom = get.getTiles();
        Map<BlockVector3, CompoundTag> tilesTo = set.getTiles();
        if (!tilesFrom.isEmpty()) {
            for (Map.Entry<BlockVector3, CompoundTag> entry : tilesFrom.entrySet()) {
                BlockVector3 pos = entry.getKey();
                BlockState fromBlock = get.getBlock(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
                BlockState toBlock = set.getBlock(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
                if (fromBlock != toBlock || tilesTo.containsKey(pos)) {
                    addTileRemove(entry.getValue());
                }
            }
        }
        if (!tilesTo.isEmpty()) {
            for (Map.Entry<BlockVector3, CompoundTag> entry : tilesTo.entrySet()) {
                addTileCreate(entry.getValue());
            }
        }
        Set<UUID> entRemoves = set.getEntityRemoves();
        if (!entRemoves.isEmpty()) {
            for (UUID uuid : entRemoves) {
                CompoundTag found = get.getEntity(uuid);
                if (found != null) {
                    addEntityRemove(found);
                }
            }
        }
        Set<CompoundTag> ents = set.getEntities();
        if (!ents.isEmpty()) {
            for (CompoundTag tag : ents) {
                addEntityCreate(tag);
            }
        }
        for (int layer = 0; layer < 16; layer++) {
            if (!set.hasSection(layer)) continue;
            // add each block and tile
            char[] blocksGet = get.load(layer);
            if (blocksGet == null) {
                blocksGet = FaweCache.IMP.EMPTY_CHAR_4096;
            }
            char[] blocksSet = set.load(layer);

            int by = layer << 4;
            for (int y = 0, index = 0; y < 16; y++) {
                int yy = y + by;
                for (int z = 0; z < 16; z++) {
                    int zz = z + bz;
                    for (int x = 0; x < 16; x++, index++) {
                        int xx = bx + x;
                        int combinedFrom = blocksGet[index];
                        if (combinedFrom == 0) {
                            combinedFrom = BlockID.AIR;
                        }
                        int combinedTo = blocksSet[index];
                        if (combinedTo != 0) {
                            add(xx, yy, zz, combinedFrom, combinedTo);
                        }
                    }
                }
            }
        }

        BiomeType[] biomes = set.getBiomes();
        if (biomes != null) {
            for (int z = 0, index = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, index++) {
                    BiomeType newBiome = biomes[index];
                    if (newBiome != null) {
                        BiomeType oldBiome = get.getBiomeType(x, z);
                        if (oldBiome != newBiome) {
                            addBiomeChange(bx + x, bz + z, oldBiome, newBiome);
                        }
                    }
                }
            }
        }
        return set;
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

    public EditSession toEditSession(Player player) {
        return toEditSession(player, null);
    }

    public EditSession toEditSession(Player player, Region[] regions) {
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
            getLogger(FaweChangeSet.class).debug("Unknown change: " + change.getClass());
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
