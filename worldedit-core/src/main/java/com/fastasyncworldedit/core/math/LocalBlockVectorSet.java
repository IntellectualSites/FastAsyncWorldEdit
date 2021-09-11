package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.math.BlockVector3;
import com.zaxxer.sparsebits.SparseBitSet;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The LocalBlockVectorSet is a Memory and CPU optimized Set for storing BlockVectors which are all in a local region
 * - All vectors must be in a 2048 * 512 * 2048 area centered around the first entry
 * - This will use 8 bytes for every 64 BlockVectors (about 800x less than a HashSet)
 */
public class LocalBlockVectorSet implements Set<BlockVector3> {

    private final SparseBitSet set;
    private int offsetX;
    private int offsetZ;

    /**
     * New LocalBlockVectorSet that will set the offset x and z to the first value given
     */
    public LocalBlockVectorSet() {
        offsetX = offsetZ = Integer.MAX_VALUE;
        this.set = new SparseBitSet();
    }

    /**
     * New LocalBlockVectorSet with a given offset
     *
     * @param x x offset
     * @param z z offset
     */
    public LocalBlockVectorSet(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
        this.set = new SparseBitSet();
    }

    private LocalBlockVectorSet(int x, int z, SparseBitSet set) {
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
     * @param y y position
     * @param z z position
     * @return if the set contains the position
     */
    public boolean contains(int x, int y, int z) {
        // take 128 to fit -256<y<255
        return set.get(MathMan.tripleSearchCoords(x - offsetX, y - 128, z - offsetZ));
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
    public LocalBlockVectorSet clone() {
        return new LocalBlockVectorSet(offsetX, offsetZ, set.clone());
    }

    /**
     * If a radius is contained by the set
     *
     * @param x      x radius center
     * @param y      y radius center
     * @param z      z radius center
     * @param radius
     * @return if radius is contained by the set
     */
    public boolean containsRadius(int x, int y, int z, int radius) {
        if (radius <= 0) {
            return contains(x, y, z);
        }
        int length = radius * 2;
        if (size() < length * length * length) {
            int index = -1;
            while ((index = set.nextSetBit(index + 1)) != -1) {
                int b1 = (index & 0xFF);
                int b2 = (index >> 8) & 0xff;
                int b3 = (index >> 15) & 0xFF;
                int b4 = (index >> 23) & 0xFF;
                int ix = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
                // Add 128 as we shift y by 128 to fit -256<y<255
                int iy = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
                int iz = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
                if (Math.abs(ix - x) <= radius && Math.abs(iz - z) <= radius && Math.abs(iy - y) <= radius) {
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

    /**
     * Set the offset applied to values when storing and reading to keep the values within -1024 to 1023
     *
     * @param x x offset
     * @param z z offset
     */
    public void setOffset(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
    }

    protected MutableBlockVector3 getIndex(int getIndex) {
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
            int b2 = (index >> 8) & 0xff;
            int b3 = (index >> 15) & 0xFF;
            int b4 = (index >> 23) & 0xFF;
            int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
            // Add 128 as we shift y by 128 to fit -256<y<255
            int y = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
            int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
            return MutableBlockVector3.get(x, y, z);
        }
        return null;
    }

    @Nonnull
    @Override
    public Iterator<BlockVector3> iterator() {
        return new Iterator<BlockVector3>() {
            final MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
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
            public BlockVector3 next() {
                if (index != -1) {
                    int b1 = (index & 0xFF);
                    int b2 = (index >> 8) & 0xff;
                    int b3 = (index >> 15) & 0xFF;
                    int b4 = (index >> 23) & 0xFF;
                    int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
                    // Add 128 as we shift y by 128 to fit -256<y<255
                    int y = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
                    int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
                    mutable.mutX(x);
                    mutable.mutY(y);
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
    public Object[] toArray() {
        return toArray((Object[]) null);
    }

    @Nonnull
    @Override
    public <T> T[] toArray(T[] array) {
        int size = size();
        if (array == null || array.length < size) {
            array = (T[]) new BlockVector3[size];
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index);
            int b1 = (index & 0xFF);
            int b2 = (index >> 8) & 0xff;
            int b3 = (index >> 15) & 0xFF;
            int b4 = (index >> 23) & 0xFF;
            int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
            // Add 128 as we shift y by 128 to fit -256<y<255
            int y = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
            int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
            array[i] = (T) BlockVector3.at(x, y, z);
            index++;
        }
        return array;
    }

    /**
     * If a position is contained by the bounds of the set
     *
     * @param x x position
     * @param y y position
     * @param z z position
     * @return true if position is contained by the bounds of the set
     */
    public boolean canAdd(int x, int y, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            return false;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            return false;
        }
        return y >= -128 && y <= 383;
    }

    /**
     * Add a position to the set if not present
     *
     * @param x x position
     * @param y y position
     * @param z z position
     * @return true if not already present
     */
    public boolean add(int x, int y, int z) {
        if (offsetX == Integer.MAX_VALUE) {
            offsetX = x;
            offsetZ = z;
        }
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            throw new UnsupportedOperationException(
                    "LocalVectorSet can only contain vectors within 1024 blocks (cuboid) of the first entry. ");
        }
        if (y < -128 || y > 383) {
            throw new UnsupportedOperationException("LocalVectorSet can only contain vectors from y elem:[-128,383]");
        }
        int index = getIndex(x, y, z);
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
    public boolean add(BlockVector3 vector) {
        return add(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private int getIndex(BlockVector3 vector) {
        // take 128 to fit -256<y<255
        return MathMan.tripleSearchCoords(
                vector.getBlockX() - offsetX,
                vector.getBlockY() - 128,
                vector.getBlockZ() - offsetZ
        );
    }

    private int getIndex(int x, int y, int z) {
        // take 128 to fit -256<y<255
        return MathMan.tripleSearchCoords(x - offsetX, y - 128, z - offsetZ);
    }

    /**
     * Remove a position from the set.
     *
     * @param x x positition
     * @param y y positition
     * @param z z positition
     * @return true if value was present.
     */
    public boolean remove(int x, int y, int z) {
        int relX = x - offsetX;
        int relZ = z - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            return false;
        }
        // take 128 to fit -256<y<255
        int index = MathMan.tripleSearchCoords(relX, y - 128, relZ);
        boolean value = set.get(index);
        set.clear(index);
        return value;
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
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean result = false;
        int size = size();
        int index = -1;
        MutableBlockVector3 mVec = MutableBlockVector3.get(0, 0, 0);
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int b1 = (index & 0xFF);
            int b2 = (index >> 8) & 0xff;
            int b3 = (index >> 15) & 0xFF;
            int b4 = (index >> 23) & 0xFF;
            int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
            // Add 128 as we shift y by 128 to fit -256<y<255
            int y = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
            int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
            mVec.mutX(x);
            mVec.mutY(y);
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
    public void forEach(BlockVectorSetVisitor visitor) {
        int size = size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index + 1);
            int b1 = (index & 0xFF);
            int b2 = (index >> 8) & 0xff;
            int b3 = (index >> 15) & 0xFF;
            int b4 = (index >> 23) & 0xFF;
            int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
            // Add 128 as we shift y by 128 to fit -256<y<255
            int y = 128 + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
            int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
            visitor.run(x, y, z, index);
        }
    }

    @Override
    public void clear() {
        offsetZ = Integer.MAX_VALUE;
        offsetX = Integer.MAX_VALUE;
        set.clear();
    }

    public interface BlockVectorSetVisitor {

        void run(int x, int y, int z, int index);

    }

}
