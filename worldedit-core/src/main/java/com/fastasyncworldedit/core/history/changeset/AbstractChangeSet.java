package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.HistoryExtent;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.util.concurrent.Futures;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This batch processor writes changes to a concrete implementation.
 * {@link #processSet(IChunk, IChunkGet, IChunkSet)} is synchronized to guarantee consistency.
 * To avoid many blocking threads on this method, changes are enqueued in {@link #queue}.
 * This allows to keep other threads free for other work.
 */
public abstract class AbstractChangeSet implements ChangeSet, IBatchProcessor {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final World world;
    private final AtomicInteger lastException = new AtomicInteger();
    private final Semaphore workerSemaphore = new Semaphore(1, false);
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    protected volatile boolean closed;

    public AbstractChangeSet(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    public void closeAsync() {
        if (closed) {
            return;
        }
        TaskManager.taskManager().async(() -> {
            try {
                close();
            } catch (IOException e) {
                LOGGER.catching(e);
            }
        });
    }

    @Override
    public void flush() {
        try {
            // drain with this thread too
            drainQueue(true);
        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            flush();
            closed = true;
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
    public final synchronized IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
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
                    addTileRemove(MainUtil.setPosition(entry.getValue(), entry.getKey().getX(), entry.getKey().getY(),
                            entry.getKey().getZ()));
                }
            }
        }
        if (!tilesTo.isEmpty()) {
            for (Map.Entry<BlockVector3, CompoundTag> entry : tilesTo.entrySet()) {
                BlockVector3 pos = entry.getKey();
                addTileCreate(MainUtil.setPosition(entry.getValue(), pos.getX() + bx, pos.getY(), pos.getZ() + bz));
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
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }
            // add each block and tile
            char[] blocksGet;
            char[] tmp = get.load(layer);
            if (tmp == null) {
                blocksGet = FaweCache.INSTANCE.EMPTY_CHAR_4096;
            } else {
                System.arraycopy(tmp, 0, (blocksGet = new char[4096]), 0, 4096);
            }
            char[] blocksSet;
            // loadIfPresent shouldn't be null if set.hasSection(layer) is true
            System.arraycopy(Objects.requireNonNull(set.loadIfPresent(layer)), 0, (blocksSet = new char[4096]), 0, 4096);

            // Account for negative layers
            int by = layer << 4;
            for (int y = 0, index = 0; y < 16; y++) {
                int yy = y + by;
                for (int z = 0; z < 16; z++) {
                    int zz = z + bz;
                    for (int x = 0; x < 16; x++, index++) {
                        final int combinedTo = blocksSet[index];
                        if (combinedTo != BlockTypesCache.ReservedIDs.__RESERVED__) {
                            int xx = bx + x;
                            int from = blocksGet[index];
                            if (from == BlockTypesCache.ReservedIDs.__RESERVED__) {
                                from = BlockTypesCache.ReservedIDs.AIR;
                            }
                            final int combinedFrom = from;
                            add(xx, yy, zz, combinedFrom, combinedTo);
                        }
                    }
                }
            }
        }

        BiomeType[][] biomes = set.getBiomes();
        if (biomes != null) {
            for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
                if (!set.hasBiomes(layer)) {
                    continue;
                }
                BiomeType[] biomeSection = biomes[layer - set.getMinSectionPosition()];
                int index = 0;
                int yy = layer << 4;
                for (int y = 0; y < 16; y+= 4) {
                    for (int z = 0; z < 16; z+= 4) {
                        for (int x = 0; x < 16; x+= 4, index++) {
                            BiomeType newBiome = biomeSection[index];
                            if (newBiome != null) {
                                BiomeType oldBiome = get.getBiomeType(x, yy + y, z);
                                if (oldBiome != newBiome) {
                                    addBiomeChange(bx + x, yy + y, bz + z, oldBiome, newBiome);
                                }
                            }
                        }
                    }
                }
            }
        }
        return set;
    }

    @Override
    public void postProcess(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        addWriteTask(() -> processSet(chunk, get, set));
    }

    @Override
    public Future<?> postProcessSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return addWriteTask(() -> processSet(chunk, get, set));
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }

    public abstract void addTileCreate(CompoundTag tag);

    public abstract void addTileRemove(CompoundTag tag);

    public abstract void addEntityRemove(CompoundTag tag);

    public abstract void addEntityCreate(CompoundTag tag);

    public abstract void addBiomeChange(int x, int y, int z, BiomeType from, BiomeType to);

    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    public abstract Iterator<Change> getIterator(boolean redo);

    public EditSession toEditSession(Actor actor) {
        return toEditSession(actor, null);
    }

    public EditSession toEditSession(Actor actor, Region[] regions) {
        EditSessionBuilder builder = WorldEdit.getInstance().newEditSessionBuilder().world(world)
                .checkMemory(false)
                .changeSetNull()
                .fastMode(false)
                .limitUnprocessed(actor)
                .actor(actor);
        if (!actor.getLimit().RESTRICT_HISTORY_TO_REGIONS) {
            builder = builder.allowedRegionsEverywhere();
        }
        EditSession editSession = builder.build();
        editSession.setSize(1);
        return editSession;
    }

    public void add(EntityCreate change) {
        CompoundTag tag = change.state.getNbtData();
        addEntityCreate(MainUtil.setEntityInfo(tag, change.getEntity()));
    }

    public void add(EntityRemove change) {
        CompoundTag tag = change.state.getNbtData();
        addEntityRemove(MainUtil.setEntityInfo(tag, change.getEntity()));
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
            LOGGER.error("Unknown change: {}", change.getClass());
        }
    }

    public void add(BlockChange change) {
        try {
            BlockVector3 loc = change.getPosition();
            BaseBlock from = change.getPrevious();
            BaseBlock to = change.getCurrent();
            add(loc, from, to);
        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty() && workerSemaphore.availablePermits() == 1 && size() == 0;
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
                addTileRemove(MainUtil.setPosition(nbt, x, y, z));
            }
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                assert nbt != null;
                addTileCreate(MainUtil.setPosition(nbt, x, y, z));
            }
            int combinedFrom = from.getOrdinal();
            int combinedTo = to.getOrdinal();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        try {
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                assert nbt != null;
                addTileCreate(MainUtil.setPosition(nbt, x, y, z));
            }
            int combinedTo = to.getInternalId();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    public Future<?> addWriteTask(Runnable writeTask) {
        return addWriteTask(writeTask, Fawe.isTickThread());
    }

    public Future<?> addWriteTask(final Runnable writeTask, final boolean completeNow) {
        Runnable wrappedTask = () -> {
            try {
                writeTask.run();
            } catch (Throwable t) {
                if (completeNow) {
                    throw t;
                } else {
                    int hash = t.getMessage().hashCode();
                    if (lastException.getAndSet(hash) != hash) {
                        LOGGER.catching(t);
                    }
                }
            }
        };
        if (completeNow) {
            wrappedTask.run();
            return Futures.immediateVoidFuture();
        } else {
            CompletableFuture<?> task = new CompletableFuture<>();
            queue.add(() -> {
                wrappedTask.run();
                task.complete(null);
            });
            // make sure changes are processed
            triggerWorker();
            return task;
        }
    }

    private void triggerWorker() {
        if (workerSemaphore.availablePermits() == 0) {
            return; // fast path to avoid additional tasks: a worker is already draining the queue
        }
        // create a new worker to drain the current queue
        Fawe.instance().getQueueHandler().async(() -> drainQueue(false));
    }

    private void drainQueue(boolean ignoreRunningState) {
        if (!workerSemaphore.tryAcquire()) {
            if (ignoreRunningState) {
                // ignoreRunningState means we want to block
                // even if another thread is already draining
                try {
                    workerSemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                return; // another thread is draining the queue already, ignore
            }
        }
        try {
            Runnable next;
            while ((next = queue.poll()) != null) { // process all tasks in the queue
                next.run();
            }
        } finally {
            workerSemaphore.release();
        }
    }

}
