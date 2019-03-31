package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * The BlockVectorSet is a Memory optimized Set for storing BlockVectors
 * - Internally it uses a map of Index->LocalBlockVectorSet
 * - All BlockVectors must be a valid world coordinate: y=[0,255],x=[-30000000,30000000],z=[-30000000,30000000]
 * - This will use ~8 bytes for every 64 BlockVectors (about 800x less than a HashSet)
 */
public class BlockVectorSet extends AbstractCollection<BlockVector3> implements Set<BlockVector3> {
    private Int2ObjectMap<LocalBlockVectorSet> localSets = new Int2ObjectOpenHashMap<>();

    @Override
    public int size() {
        int size = 0;
        for (Int2ObjectMap.Entry<LocalBlockVectorSet> entry : localSets.int2ObjectEntrySet()) {
            size += entry.getValue().size();
        }
        return size;
    }

    public BlockVector3 get(int index) {
        int count = 0;
        ObjectIterator<Int2ObjectMap.Entry<LocalBlockVectorSet>> iter = localSets.int2ObjectEntrySet().iterator();
        while (iter.hasNext()) {
            Int2ObjectMap.Entry<LocalBlockVectorSet> entry = iter.next();
            LocalBlockVectorSet set = entry.getValue();
            int size = set.size();
            int newSize = count + size;
            if (newSize > index) {
                int localIndex = index - count;
                MutableBlockVector3 pos = new MutableBlockVector3(set.getIndex(localIndex));
                if (pos != null) {
                    int pair = entry.getIntKey();
                    int cx = MathMan.unpairX(pair);
                    int cz = MathMan.unpairY(pair);
                    pos.mutX((cx << 11) + pos.getBlockX());
                    pos.mutZ((cz << 11) + pos.getBlockZ());
                    return pos;
                }
            }
            count += newSize;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (Int2ObjectMap.Entry<LocalBlockVectorSet> entry : localSets.int2ObjectEntrySet()) {
            if (!entry.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(int x, int y, int z) {
        int pair = MathMan.pair((short) (x >> 11), (short) (z >> 11));
        LocalBlockVectorSet localMap = localSets.get(pair);
        return localMap != null && localMap.contains(x & 2047, y, z & 2047);
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof BlockVector3) {
        	BlockVector3 v = (BlockVector3) o;
            return contains(v.getBlockX(), v.getBlockY(), v.getBlockZ());
        }
        return false;
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        final ObjectIterator<Int2ObjectMap.Entry<LocalBlockVectorSet>> entries = localSets.int2ObjectEntrySet().iterator();
        if (!entries.hasNext()) {
            return new ArrayList<BlockVector3>().iterator();
        }
        return new Iterator<BlockVector3>() {
            Int2ObjectMap.Entry<LocalBlockVectorSet> entry = entries.next();
            Iterator<BlockVector3> entryIter = entry.getValue().iterator();
            MutableBlockVector3 mutable = new MutableBlockVector3();

            @Override
            public void remove() {
                entryIter.remove();
            }

            @Override
            public boolean hasNext() {
                return entryIter.hasNext() || entries.hasNext();
            }

            @Override
            public BlockVector3 next() {
                while (!entryIter.hasNext()) {
                    if (!entries.hasNext()) {
                        throw new NoSuchElementException("End of iterator");
                    }
                    entry = entries.next();
                    entryIter = entry.getValue().iterator();
                }
                BlockVector3 localPos = entryIter.next();
                int pair = entry.getIntKey();
                int cx = MathMan.unpairX(pair);
                int cz = MathMan.unpairY(pair);
                return mutable.setComponents((cx << 11) + localPos.getBlockX(), localPos.getBlockY(), (cz << 11) + localPos.getBlockZ());
            }
        };
    }

    @Override
    public boolean add(BlockVector3 vector) {
        return add(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public boolean add(int x, int y, int z) {
        int pair = MathMan.pair((short) (x >> 11), (short) (z >> 11));
        LocalBlockVectorSet localMap = localSets.get(pair);
        if (localMap == null) {
            localMap = new LocalBlockVectorSet();
            localMap.setOffset(1024, 1024);
            localSets.put(pair, localMap);
        }
        return localMap.add(x & 2047, y, z & 2047);
    }

    public boolean remove(int x, int y, int z) {
        int pair = MathMan.pair((short) (x >> 11), (short) (z >> 11));
        LocalBlockVectorSet localMap = localSets.get(pair);
        if (localMap != null) {
            if (localMap.remove(x & 2047, y, z & 2047)) {
                if (localMap.isEmpty()) {
                    localSets.remove(pair);
                }
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean remove(Object o) {
        if (o instanceof BlockVector3) {
        	BlockVector3 v = (BlockVector3) o;
            return remove(v.getBlockX(), v.getBlockY(), v.getBlockZ());
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends BlockVector3> c) {
        boolean result = false;
        for (BlockVector3 v : c) {
            result |= add(v);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }


    @Override
    public void clear() {
        localSets.clear();
    }
}