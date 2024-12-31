package com.fastasyncworldedit.core.queue.implementation.preloader;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.util.FaweTimer;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.collection.MutablePair;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncPreloader implements Preloader, Runnable {

    private final ConcurrentHashMap<UUID, MutablePair<World, Set<BlockVector2>>> update;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public AsyncPreloader() {
        this.update = new ConcurrentHashMap<>();
        TaskManager.taskManager().laterAsync(this, 1);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        synchronized (update) {
            update.clear();
        }
    }

    @Override
    public void cancel(@Nonnull Actor actor) {
        cancelAndGet(actor);
    }

    private MutablePair<World, Set<BlockVector2>> cancelAndGet(@Nonnull Actor actor) {
        MutablePair<World, Set<BlockVector2>> existing = update.get(actor.getUniqueId());
        if (existing != null) {
            existing.setValue(null);
        }
        return existing;
    }

    @Override
    public void update(@Nonnull Actor actor, @Nonnull World world) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(actor);
        if (session == null) {
            return;
        }
        MutablePair<World, Set<BlockVector2>> existing = cancelAndGet(actor);
        try {
            Region region = session.getSelection(world);
            if (existing == null) {
                update.put(
                        actor.getUniqueId(),
                        existing = new MutablePair<>()
                );
            }
            synchronized (existing) { // Ensure key & value are mutated together
                existing.setKey(world);
                existing.setValue(ImmutableSet.copyOf(Iterables.limit(
                        region.getChunks(),
                        Settings.settings().QUEUE.PRELOAD_CHUNK_COUNT
                )));
            }
            synchronized (update) {
                update.notify();
            }
        } catch (IncompleteRegionException ignored) {
        }
    }

    @Override
    public void run() {
        FaweTimer timer = Fawe.instance().getTimer();
        if (cancelled.get()) {
            return;
        }
        if (update.isEmpty()) {
            TaskManager.taskManager().laterAsync(this, 1);
            return;
        }
        Iterator<Map.Entry<UUID, MutablePair<World, Set<BlockVector2>>>> plrIter = update.entrySet().iterator();
        while (timer.getTPS() > 18 && plrIter.hasNext()) {
            if (cancelled.get()) {
                return;
            }
            Map.Entry<UUID, MutablePair<World, Set<BlockVector2>>> entry = plrIter.next();
            MutablePair<World, Set<BlockVector2>> pair = entry.getValue();
            World world = pair.getKey();
            Set<BlockVector2> chunks = pair.getValue();
            if (chunks != null) {
                Iterator<BlockVector2> chunksIter = chunks.iterator();
                while (chunksIter.hasNext() && pair.getValue() == chunks) { // Ensure the queued load is still valid
                    BlockVector2 chunk = chunksIter.next();
                    if (Settings.settings().REGION_RESTRICTIONS_OPTIONS.RESTRICT_TO_SAFE_RANGE) {
                        int x = chunk.x();
                        int z = chunk.z();
                        // if any chunk coord is outside 30 million blocks
                        if (x > 1875000 || z > 1875000 || x < -1875000 || z < -1875000) {
                            continue;
                        }
                    }
                    queueLoad(world, chunk);
                }
            }
            plrIter.remove();
        }
        if (cancelled.get()) {
            return;
        }
        TaskManager.taskManager().laterAsync(this, 20);
    }

    private void queueLoad(World world, BlockVector2 chunk) {
        world.checkLoadedChunk(BlockVector3.at(chunk.x() << 4, 0, chunk.z() << 4));
    }

}
