package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.math.BlockVector2;
import com.zaxxer.sparsebits.SparseBitSet;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The LocalBlockVector2Set is a Memory and CPU optimized Set for storing BlockVector2s which are all in a local region
 * - All vectors must be in a 65534 * 65534 area centered around the first entry
 * - This will use 8 bytes for every 64 BlockVector2s (about 600x less than a HashSet)
 *
 * @since TODO
 */
public class LocalBlockVector2Set implements Set<BlockVector2> {

    private final SparseBitSet set;
    private int offsetX;
    private int offsetZ;

    /**
     * New LocalBlockVectorSet that will set the offset x and z to the first value given.
     *
     * @since TODO
     */
    public LocalBlockVector2Set() {
        offsetX = offsetZ = Integer.MAX_VALUE;
        this.set = new SparseBitSet();
    }

    /**
     * New LocalBlockVectorSet with a given offset.
     *
     * @param x x offset
     * @param z z offset
     */
    public LocalBlockVector2Set(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
        this.set = new SparseBitSet();
    }

    private LocalBlockVector2Set(int x, int z, SparseBitSet set) {
        this.offsetX = x;
        this.offsetZ = z;
        this.set = set;
    }

    @Override
    public int size() {
        return set.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * If the set contains a position
     *
     * @param x x position
     * @param z z position
     * @return if the set contains the position
     */
    public boolean contains(int x, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            return false;
        }
        short sx = (short) (x - offsetX);
        short sz = (short) (z - offsetZ);
        if (sx > 32767 || sx < -32767 || sz > 32767 || sz < -32767) {
            return false;
        }
        try {
            return set.get(MathMan.pairSearchCoords(sx, sz));
        } catch (IndexOutOfBoundsException e) {
            System.out.println(x + " " + z + "    " + sx + " " + sz);
            throw e;
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof BlockVector2 v) {
            return contains(v.getBlockX(), v.getBlockZ());
        }
        return false;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public LocalBlockVector2Set clone() {
        return new LocalBlockVector2Set(offsetX, offsetZ, set.clone());
    }

    /**
     * If a radius is contained by the set
     *
     * @param x x radius center
     * @param z z radius center
     * @return if radius is contained by the set
     */
    public boolean containsRadius(int x, int z, int radius) {
        if (radius <= 0) {
            return contains(x, z);
        }
        int length = radius * 2;
        if (size() < length * length * length) {
            int index = -1;
            while ((index = set.nextSetBit(index + 1)) != -1) {
                int ix = offsetX + MathMan.unpairSearchCoordsX(index);
                int iz = offsetZ + MathMan.unpairSearchCoordsY(index);
                if (Math.abs(ix - x) <= radius && Math.abs(iz - z) <= radius) {
                    return true;
                }
            }
            return false;
        }
        for (int xx = -radius; xx <= radius; xx++) {
            for (int zz = -radius; zz <= radius; zz++) {
                if (contains(x + xx, z + zz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set the offset applied to values when storing and reading to keep the values within -32767 to 32767.
     *
     * @param x x offset
     * @param z z offset
     */
    public void setOffset(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
    }

    protected MutableBlockVector2 getIndex(int getIndex) {
        int size = size();
        if (getIndex > size) {
            return null;
        }
        int index = -1;
        for (int i = 0; i <= getIndex; i++) {
            index = set.nextSetBit(index + 1);
        }
        if (index != -1) {
            int x = offsetX + MathMan.unpairSearchCoordsX(index);
            int z = offsetZ + MathMan.unpairSearchCoordsY(index);
            return MutableBlockVector2.get(x, z);
        }
        return null;
    }

    @Nonnull
    @Override
    public Iterator<BlockVector2> iterator() {
        return new Iterator<>() {
            final MutableBlockVector2 mutable = new MutableBlockVector2(0, 0);
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
                    int x = offsetX + MathMan.unpairSearchCoordsX(index);
                    int z = offsetZ + MathMan.unpairSearchCoordsY(index);
                    mutable.mutX(x);
                    mutable.mutZ(z);
                    previous = index;
                    index = set.nextSetBit(index + 1);
                    return mutable;
                }
                return null;
            }
        };
    }

    @Nonnull
    @Override
    public BlockVector2[] toArray() {
        return toArray(new BlockVector2[0]);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> T[] toArray(T[] array) {
        int size = size();
        if (array.length < size) {
            array = Arrays.copyOf(array, size);
        } else if (array.length > size) {
            array[size] = null; // mark as end to comply with the method contract
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            int x = offsetX + MathMan.unpairSearchCoordsX(index);
            int z = offsetZ + MathMan.unpairSearchCoordsY(index);
            array[i] = (T) BlockVector2.at(x, z);
            index++;
        }
        return array;
    }

    /**
     * If a position is contained by the bounds of the set
     *
     * @param x x position
     * @param z z position
     * @return true if position is contained by the bounds of the set
     */
    public boolean canAdd(int x, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            return false;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        return relX <= 32767 && relX >= -32767 && relZ <= 32727 && relZ >= -32767;
    }

    /**
     * Add a position to the set if not present
     *
     * @param x x position
     * @param z z position
     * @return true if not already present
     */
    public boolean add(int x, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            offsetX = x;
            offsetZ = z;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 32767 || relX < -32767 || relZ > 32767 || relZ < -32767) {
            throw new UnsupportedOperationException(
                    "LocalBlockVector2Set can only contain vectors within 32767 blocks (cuboid) of the first entry. Attempted " + "to set block at " + x + ", " + z + ". With origin " + offsetX + " " + offsetZ);
        }
        int index = getIndex(x, z);
        if (set.get(index)) {
            return false;
        } else {
            set.set(index);
            return true;
        }
    }

    /**
     * Add a position to the set if not present
     *
     * @param vector position
     * @return true if not already present
     */
    @Override
    public boolean add(BlockVector2 vector) {
        return add(vector.getBlockX(), vector.getBlockZ());
    }

    private int getIndex(BlockVector2 vector) {
        return getIndex(vector.getX(), vector.getZ());
    }

    private int getIndex(int x, int z) {
        return MathMan.pairSearchCoords((short) (x - offsetX), (short) (z - offsetZ));
    }

    /**
     * Remove a position from the set.
     *
     * @param x x position
     * @param z z position
     * @return true if value was present.
     */
    public boolean remove(int x, int z) {
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 32767 || relX < -32767 || relZ > 32767 || relZ < -32767) {
            return false;
        }
        int index = MathMan.pairSearchCoords((short) (x - offsetX), (short) (z - offsetZ));
        boolean value = set.get(index);
        set.clear(index);
        return value;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof BlockVector2 v) {
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
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean result = false;
        int size = size();
        int index = -1;
        MutableBlockVector2 mVec = MutableBlockVector2.get(0, 0);
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int x = offsetX + MathMan.unpairSearchCoordsX(index);
            int z = offsetZ + MathMan.unpairSearchCoordsY(index);
            mVec.mutX(x);
            mVec.mutZ(z);
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

    /**
     * Visit each point contained in the set
     *
     * @param visitor visitor to use
     */
    public void forEach(BlockVector2SetVisitor visitor) {
        int size = size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int x = offsetX + MathMan.unpairSearchCoordsX(index);
            int z = offsetZ + MathMan.unpairSearchCoordsY(index);
            visitor.run(x, z, index);
        }
    }

    @Override
    public void clear() {
        offsetZ = Integer.MAX_VALUE;
        offsetX = Integer.MAX_VALUE;
        set.clear();
    }

    public interface BlockVector2SetVisitor {

        void run(int x, int z, int index);

    }

}
