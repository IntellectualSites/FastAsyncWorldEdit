package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IGetBlocks;
import com.boydti.fawe.beta.Trimable;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

public class WorldChunkCache implements Trimable {
    protected final Long2ObjectLinkedOpenHashMap<WeakReference<IGetBlocks>> getCache;
    private final World world;

    protected WorldChunkCache(final World world) {
        this.world = world;
        this.getCache = new Long2ObjectLinkedOpenHashMap<>();
    }

    public World getWorld() {
        return world;
    }

    public synchronized int size() {
        return getCache.size();
    }

    public synchronized IGetBlocks get(final long index, final Supplier<IGetBlocks> provider) {
        final WeakReference<IGetBlocks> ref = getCache.get(index);
        if (ref != null) {
            final IGetBlocks blocks = ref.get();
            if (blocks != null) return blocks;
        }
        final IGetBlocks blocks = provider.get();
        getCache.put(index, new WeakReference<>(blocks));
        return blocks;
    }

    @Override
    public synchronized boolean trim(final boolean aggressive) {
        boolean result = true;
        if (!getCache.isEmpty()) {
            final ObjectIterator<Long2ObjectMap.Entry<WeakReference<IGetBlocks>>> iter = getCache.long2ObjectEntrySet().fastIterator();
            while (iter.hasNext()) {
                final Long2ObjectMap.Entry<WeakReference<IGetBlocks>> entry = iter.next();
                final WeakReference<IGetBlocks> value = entry.getValue();
                final IGetBlocks igb = value.get();
                if (igb == null) iter.remove();
                else {
                    result = false;
                    if (!aggressive) return result;
                    synchronized (igb) {
                        igb.trim();
                    }
                }
            }
        }
        return result;
    }
}