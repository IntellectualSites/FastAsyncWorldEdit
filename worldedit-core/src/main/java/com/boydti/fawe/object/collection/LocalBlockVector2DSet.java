package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector2D;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The LocalPartitionedBlockVector2DSet is a Memory and CPU optimized Set for storing Vector2Ds which are all in a local region
 * - All Vector2Ds must be within x[0,32768), y[0,32768)
 * - This will use 8 bytes for every 64 Vector2Ds (about 800x less than a HashSet)
 */
public class LocalBlockVector2DSet implements Set<BlockVector2> {
    private final SparseBitSet set;
    private final MutableBlockVector2D mutable = new MutableBlockVector2D();

    public LocalBlockVector2DSet() {
        this.set = new SparseBitSet();
    }

    public SparseBitSet getBitSet() {
        return set;
    }

    @Override
    public int size() {
        return set.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    public boolean contains(int x, int y) {
        return set.get(MathMan.pairSearchCoords(x, y));
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof BlockVector2) {
        	BlockVector2 v = (BlockVector2) o;
            return contains(v.getBlockX(), v.getBlockZ());
        }
        return false;
    }

    public boolean containsRadius(int x, int y, int radius) {
        int size = size();
        if (size == 0) return false;
        if (radius <= 0 || size == 1) {
            return contains(x, y);
        }
//        int centerIndex = MathMan.pairSearchCoords(x, y);
        int length = (radius << 1) + 1;
        if (size() < length * length) {
            int index = -1;
            int count = 0;
            while ((index = set.nextSetBit(index + 1)) != -1) {
//                if (index == centerIndex) continue;
                int curx = MathMan.unpairSearchCoordsX(index);
                int cury = MathMan.unpairSearchCoordsY(index);
                if (Math.abs(curx - x) <= radius && Math.abs(cury - y) <= radius) {
                    return true;
                }
            }
            return false;
        }
        int bcx = Math.max(0, (x - radius) >> 4);
        int bcy = Math.max(0, (y - radius) >> 4);
        int tcx = Math.min(2047, (x + radius) >> 4);
        int tcy = Math.min(2047, (y + radius) >> 4);
        for (int cy = bcy; cy <= tcy; cy++) {
            for (int cx = bcx; cx <= tcx; cx++) {
                int index = MathMan.pairSearchCoords(cx << 4, cy << 4) - 1;
                int endIndex = index + 256;
                while ((index = set.nextSetBit(index + 1)) != -1 && index <= endIndex) {
//                    if (index == centerIndex) continue;
                    int curx = MathMan.unpairSearchCoordsX(index);
                    int cury = MathMan.unpairSearchCoordsY(index);
                    if (Math.abs(curx - x) <= radius && Math.abs(cury - y) <= radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public BlockVector2 getIndex(int getIndex) {
        int size = size();
        if (getIndex > size) {
            return null;
        }
        int index = -1;
        for (int i = 0; i <= getIndex; i++) {
            index = set.nextSetBit(index + 1);
        }
        if (index != -1) {
            int x = MathMan.unpairSearchCoordsX(index);
            int y = MathMan.unpairSearchCoordsY(index);
            return mutable.setComponents(x, y).toBlockVector2();
        }
        return null;
    }

    @Override
    public Iterator<BlockVector2> iterator() {
        return new Iterator<BlockVector2>() {
            int index = set.nextSetBit(0);
            int previous = -1;

            @Override
            public void remove() {
                set.clear(previous);
            }

            @Override
            public boolean hasNext() {
                return index != -1;
            }

            @Override
            public BlockVector2 next() {
                if (index != -1) {
                    int x = MathMan.unpairSearchCoordsX(index);
                    int y = MathMan.unpairSearchCoordsY(index);
                    mutable.setComponents(x, y);
                    previous = index;
                    index = set.nextSetBit(index + 1);
                    return mutable.toBlockVector2();
                }
                return null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return toArray(null);
    }

    @Override
    public <T> T[] toArray(T[] array) {
        int size = size();
        if (array == null || array.length < size) {
            array = (T[]) new BlockVector2[size];
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index);
            int x = MathMan.unpairSearchCoordsX(index);
            int y = MathMan.unpairSearchCoordsY(index);
            array[i] = (T) new BlockVector2(x, y);
            index++;
        }
        return array;
    }

    public boolean add(int x, int y) {
        if (x < 0 || x > 32766 || y < 0 || y > 32766) {
            throw new UnsupportedOperationException("LocalVector2DSet can only contain Vector2Ds within 1024 blocks (cuboid) of the first entry. ");
        }
        int index = getIndex(x, y);
        if (set.get(index)) {
            return false;
        } else {
            set.set(index);
            return true;
        }
    }

    @Override
    public boolean add(BlockVector2 vector) {
        return add(vector.getBlockX(), vector.getBlockZ());
    }

    private int getIndex(BlockVector2 vector) {
        return MathMan.pairSearchCoords(vector.getBlockX(), vector.getBlockZ());
    }

    private int getIndex(int x, int y) {
        return MathMan.pairSearchCoords(x, y);
    }

    public boolean remove(int x, int y) {
        if (x < 0 || x > 32766 || y < 0 || y > 32766) {
            return false;
        }
        int index = MathMan.pairSearchCoords(x, y);
        boolean value = set.get(index);
        if (value) {
            set.clear(index);
        }
        return value;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof BlockVector2) {
        	BlockVector2 v = (BlockVector2) o;
            return remove(v.getBlockX(), v.getBlockZ());
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
    public boolean addAll(Collection<? extends BlockVector2> c) {
        boolean result = false;
        for (BlockVector2 v : c) {
            result |= add(v);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        int size = size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int x = MathMan.unpairSearchCoordsX(index);
            int y = MathMan.unpairSearchCoordsY(index);
            mutable.setComponents(x, y);
            if (!c.contains(mutable)) {
                result = true;
                set.clear(index);
            }
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    public void forEach(BlockVector2DSetVisitor visitor) {
        int size = size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int x = MathMan.unpairSearchCoordsX(index);
            int y = MathMan.unpairSearchCoordsY(index);
            mutable.setComponents(x, y);
            visitor.run(x, y, index);
        }
    }

    public static abstract class BlockVector2DSetVisitor {
        public abstract void run(int x, int y, int index);
    }

    @Override
    public void clear() {
        set.clear();
    }
}
