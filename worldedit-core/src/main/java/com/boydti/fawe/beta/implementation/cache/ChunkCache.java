package com.boydti.fawe.beta.implementation.cache;

import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.util.MathMan;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.lang.ref.WeakReference;

public class ChunkCache<T extends Trimable> implements IChunkCache<T> {

    protected final Long2ObjectLinkedOpenHashMap<WeakReference<T>> getCache;
    private final IChunkCache<T> delegate;

    public ChunkCache(IChunkCache<T> delegate) {
        this.getCache = new Long2ObjectLinkedOpenHashMap<>();
        this.delegate = delegate;
    }

    /**
     * Get or create the IGetBlocks
     *
     * @return cached IGetBlocks
     */
    @Override
    public synchronized T get(int x, int z) {
        long pair = MathMan.pairInt(x, z);
        final WeakReference<T> ref = getCache.get(pair);
        if (ref != null) {
            final T blocks = ref.get();
            if (blocks != null) {
                return blocks;
            }
        }
        final T blocks = newChunk(x, z);
        getCache.put(pair, new WeakReference<>(blocks));
        return blocks;
    }

    public T newChunk(int chunkX, int chunkZ) {
        return delegate.get(chunkX, chunkZ);
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        if (getCache.isEmpty()) {
            return true;
        }
        boolean result = true;
        final ObjectIterator<Long2ObjectMap.Entry<WeakReference<T>>> iter = getCache
                .long2ObjectEntrySet().fastIterator();
        while (iter.hasNext()) {
            final Long2ObjectMap.Entry<WeakReference<T>> entry = iter.next();
            final WeakReference<T> value = entry.getValue();
            final T igb = value.get();
            if (igb == null) {
                iter.remove();
            } else {
                result = false;
                if (!aggressive) {
                    return false;
                }
                synchronized (igb) {
                    igb.trim(true);
                }
            }
        }
        return result;
    }
}
