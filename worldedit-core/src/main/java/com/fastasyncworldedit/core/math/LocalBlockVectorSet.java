package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.math.BlockVector3;
import com.zaxxer.sparsebits.SparseBitSet;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * The LocalBlockVectorSet is a Memory and CPU optimized Set for storing BlockVectors which are all in a local region
 * - All vectors must be in a 2048 * 512 * 2048 area centered around the first entry
 * - This will use 8 bytes for every 64 BlockVectors (about 800x less than a HashSet)
 */
public class LocalBlockVectorSet implements BlockVector3Set {

    private final SparseBitSet set;
    private int offsetX;
    private int offsetZ;
    private int offsetY = 128;

    /**
     * New LocalBlockVectorSet that will set the offset x and z to the first value given. The y offset will default to 128 to
     * allow -64 -> 320 world height.
     */
    public LocalBlockVectorSet() {
        offsetX = offsetZ = Integer.MAX_VALUE;
        this.set = new SparseBitSet();
    }

    /**
     * New LocalBlockVectorSet with a given offset. Defaults y offset to 128.
     *
     * @param x x offset
     * @param z z offset
     */
    public LocalBlockVectorSet(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
        this.set = new SparseBitSet();
    }

    /**
     * New LocalBlockVectorSet with a given offset
     *
     * @param x x offset
     * @param y y offset
     * @param z z offset
     * @since 2.2.0
     */
    public LocalBlockVectorSet(int x, int y, int z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        this.set = new SparseBitSet();
    }

    private LocalBlockVectorSet(int x, int y, int z, SparseBitSet set) {
        this.offsetX = x;
        this.offsetY = y;
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
        return set.get(MathMan.tripleSearchCoords(x - offsetX, y - offsetY, z - offsetZ));
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof BlockVector3 v) {
            return contains(v.x(), v.y(), v.z());
        }
        return false;
    }

    @Override
    public LocalBlockVectorSet clone() {
        return new LocalBlockVectorSet(offsetX, offsetY, offsetZ, set.clone());
    }

    @Override
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
                int iy = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
                int iz = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
                if (Math.abs(ix - x) <= radius && Math.abs(iz - z) <= radius && Math.abs(iy - y) <= radius) {
                    return true;
                }
            }
            return false;
        }
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
    public void setOffset(int x, int z) {
        this.offsetX = x;
        this.offsetZ = z;
    }

    @Override
    public void setOffset(int x, int y, int z) {
        this.offsetX = x;
        this.offsetY = y;
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
            int y = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
            int z = offsetZ + (((b4 + (((b2 >> 3) & 0x7) << 8)) << 21) >> 21);
            return MutableBlockVector3.get(x, y, z);
        }
        return null;
    }

    @Nonnull
    @Override
    public Iterator<BlockVector3> iterator() {
        return new Iterator<>() {
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
                    int y = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
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
    public BlockVector3[] toArray() {
        return toArray(new BlockVector3[0]);
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
            index = set.nextSetBit(index);
            int b1 = (index & 0xFF);
            int b2 = (index >> 8) & 0xff;
            int b3 = (index >> 15) & 0xFF;
            int b4 = (index >> 23) & 0xFF;
            int x = offsetX + (((b3 + (((b2 & 0x7)) << 8)) << 21) >> 21);
            int y = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
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
        int relY = y - offsetY;
        return relY >= -256 && relY <= 255;
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
        int relY = y - offsetY;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024 || relY < -256 || relY > 255) {
            throw new UnsupportedOperationException(
                    "LocalVectorSet can only contain vectors within 1024 blocks (cuboid) of the first entry. Attempted to set " +
                            "block at " + x + ", " + y + ", " + z + ". With origin " + offsetX + " " + offsetY + " " + offsetZ);
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
        return add(vector.x(), vector.y(), vector.z());
    }

    private int getIndex(BlockVector3 vector) {
        return MathMan.tripleSearchCoords(
                vector.x() - offsetX,
                vector.y() - offsetY,
                vector.z() - offsetZ
        );
    }

    private int getIndex(int x, int y, int z) {
        return MathMan.tripleSearchCoords(x - offsetX, y - offsetY, z - offsetZ);
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
        int relY = y - offsetY;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024 || relY < -256 || relY > 255) {
            return false;
        }
        int index = MathMan.tripleSearchCoords(relX, relY, relZ);
        boolean value = set.get(index);
        set.clear(index);
        return value;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof BlockVector3) {
            BlockVector3 v = (BlockVector3) o;
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
            int y = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
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
            int y = offsetY + b1 * (((b2 >> 6) & 0x1) == 0 ? 1 : -1);
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
