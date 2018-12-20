package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The LocalBlockVectorSet is a Memory and CPU optimized Set for storing BlockVectors which are all in a local region
 * - All vectors must be in a 2048 * 2048 area centered around the first entry
 * - This will use 8 bytes for every 64 BlockVectors (about 800x less than a HashSet)
 */
public class LocalBlockVectorSet implements Set<Vector> {
    private int offsetX, offsetZ;
    private final SparseBitSet set;

    public LocalBlockVectorSet() {
        offsetX = offsetZ = Integer.MAX_VALUE;
        this.set = new SparseBitSet();
    }

    public LocalBlockVectorSet(int x, int z, SparseBitSet set) {
        this.offsetX = x;
        this.offsetZ = z;
        this.set = set;
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

    public boolean contains(int x, int y, int z) {
        return set.get(MathMan.tripleSearchCoords(x - offsetX, y, z - offsetZ));
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Vector) {
            Vector v = (Vector) o;
            return contains(v.getBlockX(), v.getBlockY(), v.getBlockZ());
        }
        return false;
    }

    @Override
    public LocalBlockVectorSet clone() {
        return new LocalBlockVectorSet(offsetX, offsetZ, set.clone());
    }

    public boolean containsRadius(int x, int y, int z, int radius) {
        if (radius <= 0) {
            return contains(x, y, z);
        }
        int length = radius * 2;
        if (size() < length * length * length) {
            int index = -1;
            while ((index = set.nextSetBit(index + 1)) != -1) {
                int b1 = (index & 0xFF);
                int b2 = ((byte) (index >> 8)) & 0x7F;
                int b3 = ((byte) (index >> 15)) & 0xFF;
                int b4 = ((byte) (index >> 23)) & 0xFF;
                if (Math.abs((offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21)) - x) <= radius && Math.abs((offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21)) - z) <= radius && Math.abs((b1) - y) <= radius) {
                    return true;
                }
            }
            return false;
        }
        for (int xx = -radius; xx <= radius; xx++) {
            for (int yy = -radius; yy <= radius; yy++) {
                for (int zz = -radius; zz <= radius; zz++) {
                    if (contains(x + xx, y + yy, z + zz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void addOffset(int x, int z) {
        this.offsetX += x;
        this.offsetZ += z;
    }

    public void setOffset(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
    }

    public Vector getIndex(int getIndex) {
        int size = size();
        if (getIndex > size) {
            return null;
        }
        int index = -1;
        for (int i = 0; i <= getIndex; i++) {
            index = set.nextSetBit(index + 1);
        }
        if (index != -1) {
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte) (index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            int x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            int y = b1;
            int z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            return MutableBlockVector.get(x, y, z);
        }
        return null;
    }

    @Override
    public Iterator<Vector> iterator() {
        return new Iterator<Vector>() {
            int index = set.nextSetBit(0);
            int previous = -1;
            MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);

            @Override
            public void remove() {
                set.clear(previous);
            }

            @Override
            public boolean hasNext() {
                return index != -1;
            }

            @Override
            public BlockVector next() {
                if (index != -1) {
                    int b1 = (index & 0xFF);
                    int b2 = ((byte) (index >> 8)) & 0x7F;
                    int b3 = ((byte) (index >> 15)) & 0xFF;
                    int b4 = ((byte) (index >> 23)) & 0xFF;
                    mutable.mutX(offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21));
                    mutable.mutY(b1);
                    mutable.mutZ(offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21));
                    previous = index;
                    index = set.nextSetBit(index + 1);
                    return mutable;
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
            array = (T[]) new BlockVector[size];
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte) (index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            int x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            int y = b1;
            int z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            array[i] = (T) new BlockVector(x, y, z);
            index++;
        }
        return array;
    }

    public boolean canAdd(int x, int y, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            return false;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            return false;
        }
        if (y < 0 || y > 256) {
            return false;
        }
        return true;
    }

    public boolean add(int x, int y, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            offsetX = x;
            offsetZ = z;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            throw new UnsupportedOperationException("LocalVectorSet can only contain vectors within 1024 blocks (cuboid) of the first entry. ");
        }
        if (y < 0 || y > 255) {
            throw new UnsupportedOperationException("LocalVectorSet can only contain vectors from y elem:[0,255]");
        }
        int index = getIndex(x, y, z);
        if (set.get(index)) {
            return false;
        } else {
            set.set(index);
            return true;
        }
    }

    @Override
    public boolean add(Vector vector) {
        return add(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private int getIndex(Vector vector) {
        return MathMan.tripleSearchCoords(vector.getBlockX() - offsetX, vector.getBlockY(), vector.getBlockZ() - offsetZ);
    }

    private int getIndex(int x, int y, int z) {
        return MathMan.tripleSearchCoords(x - offsetX, y, z - offsetZ);
    }

    public boolean remove(int x, int y, int z) {
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            return false;
        }
        int index = MathMan.tripleSearchCoords(relX, y, relZ);
        boolean value = set.get(index);
        set.clear(index);
        return value;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Vector) {
            Vector v = (Vector) o;
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
    public boolean addAll(Collection<? extends Vector> c) {
        boolean result = false;
        for (Vector v : c) {
            result |= add(v);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        int size = size();
        int index = -1;
        Vector mVec = MutableBlockVector.get(0, 0, 0);
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte) (index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            mVec.mutX(offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21));
            mVec.mutY(b1);
            mVec.mutZ(offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21));
            if (!c.contains(mVec)) {
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

    public void forEach(BlockVectorSetVisitor visitor) {
        int size = size();
        int index = -1;
        Vector mVec = MutableBlockVector.get(0, 0, 0);
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte) (index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            int x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            int y = b1;
            int z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            visitor.run(x, y, z, index);
        }
    }

    public static abstract class BlockVectorSetVisitor {
        public abstract void run(int x, int y, int z, int index);
    }

    @Override
    public void clear() {
        offsetZ = Integer.MAX_VALUE;
        offsetX = Integer.MAX_VALUE;
        set.clear();
    }
}
