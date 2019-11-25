//package com.boydti.fawe.beta.implementation;
//
//import com.boydti.fawe.beta.IChunkGet;
//import com.boydti.fawe.beta.Trimable;
//import com.boydti.fawe.util.MathMan;
//import com.sk89q.worldedit.world.World;
//import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
//import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
//import it.unimi.dsi.fastutil.objects.ObjectIterator;
//
//import java.lang.ref.WeakReference;
//
//public class ChunkCache<T extends Trimable> implements IChunkCache<T> {
//
//    protected final Long2ObjectLinkedOpenHashMap<WeakReference<T>> getCache;
//    private final IChunkCache<T> delegate;
//
//    protected ChunkCache(IChunkCache<T> delegate) {
//        this.getCache = new Long2ObjectLinkedOpenHashMap<>();
//        this.delegate = delegate;
//    }
//
//    /**
//     * Get or create the IGetBlocks
//     *
//     * @param index    chunk index {@link com.boydti.fawe.util.MathMan#pairInt(int, int)}
//     * @param provider used to create if it isn't already cached
//     * @return cached IGetBlocks
//     */
//    @Override
//    public synchronized T get(int x, int z) {
//        long pair = MathMan.pairInt(x, z);
//        final WeakReference<T> ref = getCache.get(pair);
//        if (ref != null) {
//            final T blocks = ref.get();
//            if (blocks != null) {
//                return blocks;
//            }
//        }
//        final T blocks = newChunk(x, z);
//        getCache.put(pair, new WeakReference<>(blocks));
//        return blocks;
//    }
//
//    public T newChunk(int chunkX, int chunkZ) {
//        return delegate.get(chunkX, chunkZ);
//    }
//
//    @Override
//    public synchronized boolean trim(boolean aggressive) {
//        if (getCache.size() == 0) {
//            return true;
//        }
//        boolean result = true;
//        if (!getCache.isEmpty()) {
//            final ObjectIterator<Long2ObjectMap.Entry<WeakReference<T>>> iter = getCache
//                    .long2ObjectEntrySet().fastIterator();
//            while (iter.hasNext()) {
//                final Long2ObjectMap.Entry<WeakReference<T>> entry = iter.next();
//                final WeakReference<T> value = entry.getValue();
//                final T igb = value.get();
//                if (igb == null) {
//                    iter.remove();
//                } else {
//                    result = false;
//                    if (!aggressive) {
//                        return false;
//                    }
//                    synchronized (igb) {
//                        igb.trim(true);
//                    }
//                }
//            }
//        }
//        return result;
//    }
//}
