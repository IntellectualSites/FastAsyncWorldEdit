package com.boydti.fawe.beta.implementation.cache.preloader;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.collection.MutablePair;
import com.boydti.fawe.util.FaweTimer;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class AsyncPreloader implements Preloader, Runnable {
    private final ConcurrentHashMap<UUID, MutablePair<World, Set<BlockVector2>>> update;

    public AsyncPreloader() {
        this.update = new ConcurrentHashMap<>();
        Fawe.get().getQueueHandler().async(this);
    }

    @Override
    public void cancel(Player player) {
        cancelAndGet(player);
    }

    private MutablePair<World, Set<BlockVector2>> cancelAndGet(Player player) {
        MutablePair<World, Set<BlockVector2>> existing = update.get(player.getUniqueId());
        if (existing != null) {
            existing.setValue(null);
        }
        return existing;
    }

    @Override
    public void update(Player player) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(player);
        if (session == null) return;
        World world = player.getWorld();
        MutablePair<World, Set<BlockVector2>> existing = cancelAndGet(player);
        try {
            Region region = session.getSelection(world);
            if (!(region instanceof CuboidRegion) || region.getArea() > 50466816) {
                // TOO LARGE or NOT CUBOID
                return;
            }
            if (existing == null) {
                MutablePair<World, Set<BlockVector2>> previous = update.putIfAbsent(player.getUniqueId(), existing = new MutablePair<>());
                if (previous != null) {
                    existing = previous;
                }
                synchronized (existing) { // Ensure key & value are mutated together
                    existing.setKey(world);
                    existing.setValue(region.getChunks());
                }
                synchronized (update) {
                    update.notify();
                }
            }
        } catch (IncompleteRegionException ignore){}
    }

    @Override
    public void run() {
        FaweTimer timer = Fawe.get().getTimer();
        try {
            while (true) {
                if (!update.isEmpty()) {
                    if (timer.getTPS() > 19) {
                        Iterator<Map.Entry<UUID, MutablePair<World, Set<BlockVector2>>>> plrIter = update.entrySet().iterator();
                        Map.Entry<UUID, MutablePair<World, Set<BlockVector2>>> entry = plrIter.next();
                        MutablePair<World, Set<BlockVector2>> pair = entry.getValue();
                        World world = pair.getKey();
                        Set<BlockVector2> chunks = pair.getValue();
                        if (chunks != null) {
                            Iterator<BlockVector2> chunksIter = chunks.iterator();
                            while (chunksIter.hasNext() && pair.getValue() == chunks) { // Ensure the queued load is still valid
                                BlockVector2 chunk = chunksIter.next();
                                queueLoad(world, chunk);
                            }
                        }
                        plrIter.remove();
                    } else {
                        Thread.sleep(1000);
                    }
                } else {
                    synchronized (update) {
                        update.wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    public void queueLoad(World world, BlockVector2 chunk) {
        world.checkLoadedChunk(BlockVector3.at(chunk.getX() << 4, 0, chunk.getZ() << 4));
    }
}