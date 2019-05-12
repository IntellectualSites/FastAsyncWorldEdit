package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.Trimable;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/**
 * IGetBlocks may be cached by the WorldChunkCache so that it can be used between multiple IQueueExtents
 *  - avoids conversion between palette and raw data on every block get
 */
public class WorldChunkCache implements Trimable {
    protected final Long2ObjectLinkedOpenHashMap<WeakReference<IChunkGet>> getCache;
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

    /**
     * Get or create the IGetBlocks
     * @param index chunk index {@link com.boydti.fawe.util.MathMan#pairInt(int, int)}
     * @param provider used to create if it isn't already cached
     * @return cached IGetBlocks
     */
    public synchronized IChunkGet get(final long index, final Supplier<IChunkGet> provider) {
        final WeakReference<IChunkGet> ref = getCache.get(index);
        if (ref != null) {
            final IChunkGet blocks = ref.get();
            if (blocks != null) return blocks;
        }
        final IChunkGet blocks = provider.get();
        getCache.put(index, new WeakReference<>(blocks));
        return blocks;
    }

    @Override
    public synchronized boolean trim(final boolean aggressive) {
        boolean result = true;
        if (!getCache.isEmpty()) {
            final ObjectIterator<Long2ObjectMap.Entry<WeakReference<IChunkGet>>> iter = getCache.long2ObjectEntrySet().fastIterator();
            while (iter.hasNext()) {
                final Long2ObjectMap.Entry<WeakReference<IChunkGet>> entry = iter.next();
                final WeakReference<IChunkGet> value = entry.getValue();
                final IChunkGet igb = value.get();
                if (igb == null) iter.remove();
                else {
                    result = false;
                    if (!aggressive) return result;
                    synchronized (igb) {
                        igb.trim(aggressive);
                    }
                }
            }
        }
        return result;
    }
}