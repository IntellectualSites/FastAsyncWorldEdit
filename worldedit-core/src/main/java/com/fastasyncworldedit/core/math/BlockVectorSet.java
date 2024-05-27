package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The BlockVectorSet is a memory optimized Set for storing {@link BlockVector3}'s.
 *
 * <p>
 * It uses about 8 bytes of memory for every 64 {@code BlockVector3}s (about 800 times less than a
 * {@code HashSet}.
 * </p>
 */
public class BlockVectorSet extends AbstractCollection<BlockVector3> implements BlockVector3Set {

    private final Long2ObjectLinkedOpenHashMap<LocalBlockVectorSet> localSets = new Long2ObjectLinkedOpenHashMap<>(4);

    @Override
    public int size() {
        int size = 0;
        for (Long2ObjectMap.Entry<LocalBlockVectorSet> entry : localSets.long2ObjectEntrySet()) {
            size += entry.getValue().size();
        }
        return size;
    }

    public BlockVector3 get(int index) {
        int count = 0;
        for (Long2ObjectMap.Entry<LocalBlockVectorSet> entry : localSets.long2ObjectEntrySet()) {
            LocalBlockVectorSet set = entry.getValue();
            int size = set.size();
            int newSize = count + size;
            if (newSize > index) {
                int localIndex = index - count;
                MutableBlockVector3 pos = set.getIndex(localIndex);
                if (pos != null) {
                    long triple = entry.getLongKey();
                    int cx = (int) MathMan.untripleWorldCoordX(triple);
                    int cy = (int) MathMan.untripleWorldCoordY(triple);
                    int cz = (int) MathMan.untripleWorldCoordZ(triple);
                    pos.mutX((cx << 11) + pos.x());
                    pos.mutY((cy << 9) + pos.y());
                    pos.mutZ((cz << 11) + pos.z());
                    return pos.toImmutable();
                }
            }
            count += newSize;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (Long2ObjectMap.Entry<LocalBlockVectorSet> entry : localSets.long2ObjectEntrySet()) {
            if (!entry.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(int x, int y, int z) {
        int indexedY = (y + 128) >> 9;
        long triple = MathMan.tripleWorldCoord((x >> 11), indexedY, (z >> 11));
        LocalBlockVectorSet localMap = localSets.get(triple);
        return localMap != null && localMap.contains(x & 2047, ((y + 128) & 511) - 128, z & 2047);
    }

    @Override
    public void setOffset(final int x, final int z) {
        // Do nothing
    }

    @Override
    public void setOffset(final int x, final int y, final int z) {
        // Do nothing
    }

    @Override
    public boolean containsRadius(final int x, final int y, final int z, final int radius) {
        if (radius <= 0) {
            return contains(x, y, z);
        }
        // Quick corners check
        if (!contains(x - radius, y, z - radius)) {
            return false;
        }
        if (!contains(x + radius, y, z + radius)) {
            return false;
        }
        if (!contains(x - radius, y, z + radius)) {
            return false;
        }
        if (!contains(x + radius, y, z - radius)) {
            return false;
        }
        // Slow but if someone wants to think of an elegant way then feel free to add it
        for (int xx = -radius; xx <= radius; xx++) {
            int rx = x + xx;
            for (int yy = -radius; yy <= radius; yy++) {
                int ry = y + yy;
                for (int zz = -radius; zz <= radius; zz++) {
                    if (contains(rx, ry, z + zz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof BlockVector3 v) {
            return contains(v.x(), v.y(), v.z());
        }
        return false;
    }

    @Nonnull
    @Override
    public Iterator<BlockVector3> iterator() {
        final ObjectIterator<Long2ObjectMap.Entry<LocalBlockVectorSet>> entries = localSets.long2ObjectEntrySet().iterator();
        if (!entries.hasNext()) {
            return Collections.emptyIterator();
        }
        return new Iterator<>() {
            Long2ObjectMap.Entry<LocalBlockVectorSet> entry = entries.next();
            Iterator<BlockVector3> entryIter = entry.getValue().iterator();
            final MutableBlockVector3 mutable = new MutableBlockVector3();

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
                long triple = entry.getLongKey();
                int cx = (int) MathMan.untripleWorldCoordX(triple);
                int cy = (int) MathMan.untripleWorldCoordY(triple);
                int cz = (int) MathMan.untripleWorldCoordZ(triple);
                return mutable.setComponents(
                        (cx << 11) + localPos.x(),
                        (cy << 9) + localPos.y(),
                        (cz << 11) + localPos.z()
                );
            }
        };
    }

    @Override
    public boolean add(BlockVector3 vector) {
        return add(vector.x(), vector.y(), vector.z());
    }

    public boolean add(int x, int y, int z) {
        int indexedY = (y + 128) >> 9;
        long triple = MathMan.tripleWorldCoord((x >> 11), indexedY, (z >> 11));
        LocalBlockVectorSet localMap = localSets.get(triple);
        if (localMap == null) {
            localMap = new LocalBlockVectorSet();
            localMap.setOffset(1024, 1024);
            localSets.put(triple, localMap);
        }
        return localMap.add(x & 2047, ((y + 128) & 511) - 128, z & 2047);
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
        if (o instanceof BlockVector3 v) {
            return remove(v.x(), v.y(), v.z());
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
    public boolean retainAll(@Nonnull Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<BlockVector3> it = iterator();
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
