package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.implementation.SingleThreadQueueExtent;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.sk89q.worldedit.world.World;

public class BukkitQueue extends SingleThreadQueueExtent {

    @Override
    public synchronized void init(WorldChunkCache cache) {
        World world = cache.getWorld();
        super.init(cache);
    }

    private static final IterableThreadLocal<BukkitFullChunk> FULL_CHUNKS = new IterableThreadLocal<BukkitFullChunk>() {
        @Override
        public BukkitFullChunk init() {
            return new BukkitFullChunk();
        }
    };

    @Override
    public IChunk create(boolean full) {
        if (full) {
            return FULL_CHUNKS.get();
        } else {
            return new BukkitChunkHolder();
        }
    }
}
