package com.boydti.fawe.bukkit.v1_13.beta;

import com.boydti.fawe.bukkit.v1_13.IQueueExtent;
import com.boydti.fawe.bukkit.v1_13.beta.holder.ChunkHolder;
import com.boydti.fawe.bukkit.v1_13.beta.holder.IDelegateChunk;
import com.boydti.fawe.bukkit.v1_13.beta.holder.ReferenceChunk;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SingleThreadQueueExtent implements IQueueExtent {
    private WorldWrapper wrapped;
    private World world;

    public World getWorld() {
        return world;
    }

    public WorldWrapper getWrappedWorld() {
        return wrapped;
    }

    private void reset() {
        wrapped = null;
        world = null;
        chunks.clear();
        lastChunk = null;
        lastPair = Long.MAX_VALUE;
    }

    public synchronized void init(World world) {
        if (world != null) {
            reset();
        }
        checkNotNull(world);
        if (world instanceof EditSession) {
            world = ((EditSession) world).getWorld();
        }
        checkNotNull(world);
        if (world instanceof WorldWrapper) {
            this.wrapped = (WorldWrapper) world;
            world = WorldWrapper.unwrap(world);
        } else {
            this.wrapped = WorldWrapper.wrap(world);
        }
        this.world = world;
    }

    private IChunk lastChunk;
    private long lastPair = Long.MAX_VALUE;
    private final Long2ObjectLinkedOpenHashMap<IDelegateChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();

    private final IDelegateChunk getCachedChunk2(long pair) {
        IDelegateChunk chunk = chunks.get(pair);
        if (chunk instanceof ReferenceChunk) {
            chunk = (ReferenceChunk) (chunk).getParent();
        }
        if (chunk != null) {
            lastPair = pair;
            lastChunk = chunk;
        }
        return chunk;
    }

    public final IChunk getCachedChunk(int X, int Z) {
        long pair = MathMan.pairInt(X, Z);
        if (pair == lastPair) {
            return lastChunk;
        }

        IDelegateChunk chunk = getCachedChunk2(pair);
        if (chunk != null) return chunk;

        chunk = getCachedChunk2(pair);
        if (chunk != null) return chunk;

        int size = chunks.size();
        if (size > Settings.IMP.QUEUE.TARGET_SIZE || MemUtil.isMemoryLimited()) {
            if (size > Settings.IMP.QUEUE.PARALLEL_THREADS * 2 + 16) {
                chunk = chunks.removeFirst();
                chunk.apply();
                chunk = (IDelegateChunk) chunk.findParent(ChunkHolder.class);
                chunk.init(this, X, Z);
            } else {
                chunk = create(false);
            }
        } else {
            chunk = create(false);
        }
        chunk = wrap(chunk);

        chunks.put(pair, chunk);
        lastPair = pair;
        lastChunk = chunk;

        return chunk;
    }
}