package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.HistoryExtent;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.fastasyncworldedit.core.util.NbtUtils;
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
import org.enginehub.linbus.tree.LinCompoundTag;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
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

        Map<BlockVector3, FaweCompoundTag> tilesFrom = get.tiles();
        Map<BlockVector3, FaweCompoundTag> tilesTo = set.tiles();
        if (!tilesFrom.isEmpty()) {
            for (Map.Entry<BlockVector3, FaweCompoundTag> entry : tilesFrom.entrySet()) {
                BlockVector3 pos = entry.getKey();
                BlockState fromBlock = get.getBlock(pos.x() & 15, pos.y(), pos.z() & 15);
                BlockState toBlock = set.getBlock(pos.x() & 15, pos.y(), pos.z() & 15);
                if (fromBlock != toBlock || tilesTo.containsKey(pos)) {
                    addTileRemove(NbtUtils.withPosition(entry.getValue(), entry.getKey().x(), entry.getKey().y(),
                            entry.getKey().z()
                    ));
                }
            }
        }
        if (!tilesTo.isEmpty()) {
            for (Map.Entry<BlockVector3, FaweCompoundTag> entry : tilesTo.entrySet()) {
                BlockVector3 pos = entry.getKey();
                addTileCreate(NbtUtils.withPosition(entry.getValue(), pos.x() + bx, pos.y(), pos.z() + bz));
            }
        }
        Set<UUID> entRemoves = set.getEntityRemoves();
        if (!entRemoves.isEmpty()) {
            for (UUID uuid : entRemoves) {
                FaweCompoundTag found = get.entity(uuid);
                if (found != null) {
                    addEntityRemove(found);
                }
            }
        }
        Collection<FaweCompoundTag> ents = set.entities();
        if (!ents.isEmpty()) {
            for (FaweCompoundTag tag : ents) {
                addEntityCreate(tag);
            }
        }
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }

            // add each block and tile
            DataArray blocksGet;
            DataArray tmpGet = get.load(layer);
            if (tmpGet == null) {
                blocksGet = FaweCache.INSTANCE.EMPTY_DATA;
            } else {
                blocksGet = DataArray.createCopy(tmpGet);
            }
            // assume "set" is a copy and doesn't get modified further
            // loadIfPresent shouldn't be null if set.hasSection(layer) is true
            DataArray blocksSet = Objects.requireNonNull(set.loadIfPresent(layer));

            // Account for negative layers
            int by = layer << 4;
            for (int y = 0, index = 0; y < 16; y++) {
                int yy = y + by;
                for (int z = 0; z < 16; z++) {
                    int zz = z + bz;
                    for (int x = 0; x < 16; x++, index++) {
                        final int combinedTo = blocksSet.getAt(index);
                        if (combinedTo != BlockTypesCache.ReservedIDs.__RESERVED__) {
                            int xx = bx + x;
                            int from = blocksGet.getAt(index);
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
                for (int y = 0; y < 16; y += 4) {
                    for (int z = 0; z < 16; z += 4) {
                        for (int x = 0; x < 16; x += 4, index++) {
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
        return ProcessorScope.READING_BLOCKS;
    }

    @Deprecated(forRemoval = true, since = "2.11.2")
    public void addTileCreate(CompoundTag tag) {
        addTileCreate(adapt(tag));
    }

    @SuppressWarnings({"deprecation"})
    private static @Nonnull FaweCompoundTag adapt(CompoundTag tag) {
        return FaweCompoundTag.of(tag.toLinTag());
    }

    /**
     * Creates a tile/block entity create change to this change set.
     *
     * @param tag the tile/block entity to add.
     * @since 2.11.2
     */
    public abstract void addTileCreate(FaweCompoundTag tag);

    @Deprecated(forRemoval = true, since = "2.11.2")
    public void addTileRemove(CompoundTag tag) {
        addTileRemove(adapt(tag));
    }

    /**
     * Creates a tile/block entity remove change to this change set.
     *
     * @param tag the tile/block entity to remove.
     * @since 2.11.2
     */
    public abstract void addTileRemove(FaweCompoundTag tag);

    @Deprecated(forRemoval = true, since = "2.11.2")
    public void addEntityRemove(CompoundTag tag) {
        addEntityRemove(adapt(tag));
    }

    /**
     * Creates an entity remove change to this change set.
     *
     * @param tag the entity to remove.
     * @since 2.11.2
     */
    public abstract void addEntityRemove(FaweCompoundTag tag);

    @Deprecated(forRemoval = true, since = "2.11.2")
    public void addEntityCreate(CompoundTag tag) {
        addEntityCreate(adapt(tag));
    }

    /**
     * Creates an entity create change to this change set.
     *
     * @param tag the entity to add.
     * @since 2.11.2
     */
    public abstract void addEntityCreate(FaweCompoundTag tag);

    public abstract void addBiomeChange(int x, int y, int z, BiomeType from, BiomeType to);

    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    /**
     * {@return a coordinator to exchange sets of changes between a producer and a consumer}
     * @since 2.11.2
     */
    @ApiStatus.Internal
    public abstract ChangeExchangeCoordinator getCoordinatedChanges(BlockBag blockBag, int mode, boolean dir);

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
        LinCompoundTag tag = change.state.getNbt();
        assert tag != null;
        addEntityCreate(FaweCompoundTag.of(NbtUtils.withEntityInfo(tag, change.getEntity())));
    }

    public void add(EntityRemove change) {
        LinCompoundTag tag = change.state.getNbt();
        assert tag != null;
        addEntityRemove(FaweCompoundTag.of(NbtUtils.withEntityInfo(tag, change.getEntity())));
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
            BlockVector3 loc = change.position();
            BaseBlock from = change.previous();
            BaseBlock to = change.current();
            add(loc, from, to);
        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty() && workerSemaphore.availablePermits() == 1 && longSize() == 0;
    }

    public void add(BlockVector3 loc, BaseBlock from, BaseBlock to) {
        int x = loc.x();
        int y = loc.y();
        int z = loc.z();
        add(x, y, z, from, to);
    }

    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        try {
            LinCompoundTag nbt = from.getNbt();
            if (nbt != null) {
                addTileRemove(FaweCompoundTag.of(NbtUtils.withPosition(nbt, x, y, z)));
            }
            nbt = to.getNbt();
            if (nbt != null) {
                addTileCreate(FaweCompoundTag.of(NbtUtils.withPosition(nbt, x, y, z)));
            }
            int combinedFrom = from.getOrdinal();
            int combinedTo = to.getOrdinal();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        try {
            LinCompoundTag nbt = to.getNbt();
            if (nbt != null) {
                addTileCreate(FaweCompoundTag.of(NbtUtils.withPosition(nbt, x, y, z)));
            }
            int combinedTo = to.getInternalId();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    public Future<?> addWriteTask(Runnable writeTask) {
        return addWriteTask(writeTask, Fawe.isMainThread());
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
