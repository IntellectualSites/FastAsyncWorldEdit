package com.boydti.fawe.object.collection;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Memory optimized BlockVector3 Set using a sparsely populated bitset and grouped by chunk section
 * Note on spaghetti code / duplication
 *  - Uses a minimum of 1 bit per entry
 *  - 99.9% of the time there are no if checks on get/clear
 *  - Grouping / iteration is by chunk section, and the y>z>x order
 */
public final class MemBlockSet extends BlockSet {
    public final static int BITS_PER_WORD = 6;
    public final static int WORDS = FaweCache.BLOCKS_PER_LAYER >> BITS_PER_WORD;
    public final static IRow NULL_ROW_X = new NullRowX();
    public final static IRow NULL_ROW_Z = new NullRowZ();
    public final static IRow NULL_ROW_Y = new NullRowY();
    public final IRow[] rows;
    public final MutableBlockVector3 mutable;

    public MemBlockSet() {
        this(16, 0, 0);
    }

    public MemBlockSet(int size, int offsetX, int offsetZ) {
        super(offsetX, offsetZ);
        this.rows = new IRow[size];
        for (int i = 0; i < size; i++) rows[i] = NULL_ROW_X;
        this.mutable = new MutableBlockVector3();
    }

    @Override
    public boolean contains(int x, int y, int z) {
        x -= getBlockOffsetX();
        z -= getBlockOffsetZ();
        return rows[x >> 4].get(this.rows, x, y, z - getBlockOffsetZ());
    }

    @Override
    public boolean add(int x, int y, int z) {
        x -= getBlockOffsetX();
        z -= getBlockOffsetZ();
        return rows[x >> 4].add(this.rows, x, y, z - getBlockOffsetZ());
    }

    @Override
    public void set(int x, int y, int z) {
        x -= getBlockOffsetX();
        z -= getBlockOffsetZ();
        rows[x >> 4].set(this.rows, x, y, z - getBlockOffsetZ());
    }

    @Override
    public void clear(int x, int y, int z) {
        x -= getBlockOffsetX();
        z -= getBlockOffsetZ();
        rows[x >> 4].clear(this.rows, x, y, z - getBlockOffsetZ());
    }

    @Override
    public boolean remove(int x, int y, int z) {
        x -= getBlockOffsetX();
        z -= getBlockOffsetZ();
        return rows[x >> 4].remove(this.rows, x, y, z - getBlockOffsetZ());
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(getMinX(), getMinimumY(), getMinZ());
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(getMaxX(), getMaximumY(), getMaxZ());
    }

    @Override
    public Set<BlockVector2> getChunks() {
        return new AbstractSet<BlockVector2>() {
            @NotNull
            @Override
            public Iterator<BlockVector2> iterator() {
                return new Iterator<BlockVector2>() {
                    private MutableBlockVector2 mutable = new MutableBlockVector2();
                    private boolean hasNext;
                    private int X,Z;
                    private int setX, setZ;

                    {
                        init();
                    }

                    private void init() {
                        for (;X < rows.length; X++) {
                            IRow nullRowX = rows[X];
                            if (nullRowX instanceof RowX) {
                                RowX rowx = (RowX) nullRowX;
                                for (;Z < rowx.rows.length; Z++) {
                                    IRow nullRowZ = rowx.rows[Z];
                                    if (nullRowZ instanceof RowZ) {
                                        setX = X;
                                        setZ = Z;
                                        Z++;
                                        hasNext = true;
                                        return;
                                    }
                                }
                                Z = 0;
                            }
                        }
                        hasNext = false;
                    }

                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public BlockVector2 next() {
                        mutable.setComponents(setX + getBlockOffsetX(), setZ + getBlockOffsetZ());
                        init();
                        return mutable;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("This set is immutable.");
                    }
                };
            }

            @Override
            public int size() {
                int size = 0;
                for (IRow nullRowX : rows) {
                    if (nullRowX instanceof RowX) {
                        RowX rowx = (RowX) nullRowX;
                        for (int Z = 0; Z < rowx.rows.length; Z++) {
                            IRow nullRowZ = rowx.rows[Z];
                            if (nullRowZ instanceof RowZ) {
                                size++;
                            }
                        }
                    }
                }
                return size;
            }

            @Override
            public boolean isEmpty() {
                return MemBlockSet.this.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof BlockVector2) {
                    BlockVector2 other = (BlockVector2) o;
                    IRow rowx = rows[other.getX() - getChunkOffsetX()];
                    if (rowx instanceof RowX) {
                        return ((RowX) rowx).rows[other.getZ() - getChunkOffsetZ()] instanceof RowZ;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public Set<BlockVector3> getChunkCubes() {
        return new AbstractSet<BlockVector3>() {
            @NotNull
            @Override
            public Iterator<BlockVector3> iterator() {
                return new Iterator<BlockVector3>() {
                    private MutableBlockVector3 mutable = new MutableBlockVector3();
                    private boolean hasNext;
                    private int X, Z, Y;
                    private int setX, setY, setZ;

                    {
                        init();
                    }

                    private void init() {
                        for (;X < rows.length; X++) {
                            IRow nullRowX = rows[X];
                            if (nullRowX instanceof RowX) {
                                RowX rowx = (RowX) nullRowX;
                                for (;Z < rowx.rows.length; Z++) {
                                    IRow nullRowZ = rowx.rows[Z];
                                    if (nullRowZ instanceof RowZ) {
                                        RowZ rowz = (RowZ) nullRowZ;
                                        for (;Y < rowz.rows.length;Y++) {
                                            IRow nullRowY = rowz.rows[Y];
                                            if (nullRowY instanceof RowY) {
                                                setX = X;
                                                setY = Y;
                                                setZ = Z;
                                                Z++;
                                                hasNext = true;
                                            }
                                        }
                                        Y = 0;
                                    }
                                }
                                Z = 0;
                            }
                        }
                        hasNext = false;
                    }

                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public BlockVector3 next() {
                        mutable.setComponents(setX + getBlockOffsetX(), setY, setZ + getBlockOffsetX());
                        init();
                        return mutable;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("This set is immutable.");
                    }
                };
            }

            @Override
            public int size() {
                int size = 0;
                for (IRow nullRowX : rows) {
                    if (nullRowX instanceof RowX) {
                        RowX rowx = (RowX) nullRowX;
                        for (int Z = 0; Z < rowx.rows.length; Z++) {
                            IRow nullRowZ = rowx.rows[Z];
                            if (nullRowZ instanceof RowZ) {
                                RowZ rowz = (RowZ) nullRowZ;
                                for (int Y = 0; Y < rowz.rows.length; Y++) {
                                    IRow nullRowY = rowz.rows[Y];
                                    if (nullRowY instanceof RowY) {
                                        size++;
                                    }
                                }
                            }
                        }
                    }
                }
                return size;
            }

            @Override
            public boolean isEmpty() {
                return MemBlockSet.this.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof BlockVector3) {
                    BlockVector3 other = (BlockVector3) o;
                    IRow rowx = rows[other.getX() - getChunkOffsetX()];
                    if (rowx instanceof RowX) {
                        IRow rowz = ((RowX) rowx).rows[other.getZ()];
                        if (rowz instanceof RowZ) {
                            return ((RowZ) rowz).rows[other.getY() - getChunkOffsetZ()] instanceof RowY;
                        }
                    }
                }
                return false;
            }
        };
    }

    public int getMinimumY() {
        int maxY = 15;
        int maxy = 16;
        int by = Integer.MAX_VALUE;
        for (IRow nullRowX : rows) {
            if (!(nullRowX instanceof RowX))
                continue;
            RowX rowx = (RowX) nullRowX;
            for (int Z = 0; Z < rowx.rows.length; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ))
                    continue;
                RowZ rowz = (RowZ) nullRowZ;
                outer:
                for (int Y = 0; Y <= maxY; Y++) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY))
                        continue;
                    RowY rowY = (RowY) nullRowY;
                    int localMaxy = Y == maxY ? maxy : 15;
                    for (int y = 0, i = 0; y < localMaxy; y++) {
                        for (int xz = 0; xz < 4; xz++, i++) {
                            long val = rowY.bits[i];
                            if (val != 0) {
                                if (y == 0) {
                                    maxY = Y - 1;
                                    maxy = 16;
                                } else {
                                    maxY = Y;
                                    maxy = y;
                                }
                                by = (Y << 4) + y;
                                if (by == 0)
                                    return 0;
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        return by;
    }

    public int getMaximumY() {
        int maxY = 0;
        int maxy = 0;
        int by = Integer.MIN_VALUE;
        for (IRow nullRowX : rows) {
            if (!(nullRowX instanceof RowX))
                continue;
            RowX rowx = (RowX) nullRowX;
            for (int Z = 0; Z < rowx.rows.length; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ))
                    continue;
                RowZ rowz = (RowZ) nullRowZ;
                outer:
                for (int Y = 15; Y >= maxY; Y--) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY))
                        continue;
                    RowY rowY = (RowY) nullRowY;
                    int localMaxy = Y == maxY ? maxy : 0;
                    for (int y = 15, i = 63; y >= localMaxy; y--) {
                        for (int xz = 3; xz >= 0; xz--, i--) {
                            long val = rowY.bits[i];
                            if (val != 0) {
                                if (y == 15) {
                                    maxY = Y + 1;
                                    maxy = 0;
                                } else {
                                    maxY = Y;
                                    maxy = y + 1;
                                }
                                by = (Y << 4) + y;
                                if (by == FaweCache.worldMaxY)
                                    return FaweCache.worldMaxY;
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        return by;
    }

    public int getMaxZ() {
        int maxChunkZ = 0;
        int maxz = -1;
        int tz = Integer.MIN_VALUE;
        for (int X = rows.length - 1; X >= 0; X--) {
            IRow nullRowX = rows[X];
            if (!(nullRowX instanceof RowX)) continue;
            RowX rowx = (RowX) nullRowX;
            outer:
            for (int Z = rowx.rows.length - 1; Z >= maxChunkZ ; Z--) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ)) continue;
                RowZ rowz = (RowZ) nullRowZ;
                if (Z != maxChunkZ) {
                    maxChunkZ = Z;
                    maxz = -1;
                }
                for (int Y = rowz.rows.length - 1; Y >= 0; Y--) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY)) continue;
                    RowY rowY = (RowY) nullRowY;
                    for (int y = 15, i1 = 63; y >= 0; y--, i1 -= 4) {
                        for (int z = 12, i = i1; z > maxz - 3; z -= 4, i--) {
                            long bitBuffer = rowY.bits[i];
                            if (bitBuffer != 0) {
                                int highest = highestBit(bitBuffer);
                                maxz = z + (highest >> 4);
                                if (maxz == 15) {
                                    tz = (maxChunkZ << 4) + 15;
                                    maxChunkZ++;
                                    break outer;
                                } else {
                                    tz = Math.max(tz, (maxChunkZ << 4) + maxz);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        return tz + getBlockOffsetZ();
    }

    public int getMaxX() {
        for (int X = rows.length - 1; X >= 0; X--) {
            IRow nullRowX = rows[X];
            if (!(nullRowX instanceof RowX)) continue;
            int tx = (X << 4);
            RowX rowx = (RowX) nullRowX;
            long or = 0;
            for (int Z = rowx.rows.length - 1; Z >= 0 ; Z--) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ)) continue;
                RowZ rowz = (RowZ) nullRowZ;
                for (int Y = rowz.rows.length - 1; Y >= 0; Y--) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY)) continue;
                    RowY rowY = (RowY) nullRowY;
                    or |= Arrays.stream(rowY.bits).reduce(0, (a, b) -> a | b);
                    or = (or & 0xFFFF) | ((or >> 16) & 0xFFFF) | ((or >> 32) & 0xFFFF) | ((or >> 48) & 0xFFFF);
                    if (highestBit(or) == 15) return tx + 15;
                }
            }
            int highest = highestBit(or);
            if (highest != 64) {
                return tx + highest + getBlockOffsetX();
            }
        }
        return Integer.MAX_VALUE;
    }

    public int getMinZ() {
        int maxChunkZ = rows.length - 1;
        int maxz = 16;
        int bz = Integer.MAX_VALUE;
        for (IRow nullRowX : rows) {
            if (!(nullRowX instanceof RowX))
                continue;
            RowX rowx = (RowX) nullRowX;
            outer:
            for (int Z = 0; Z <= maxChunkZ; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ))
                    continue;
                RowZ rowz = (RowZ) nullRowZ;
                if (Z != maxChunkZ) {
                    maxChunkZ = Z;
                    maxz = 16;
                }
                for (int Y = 0; Y < rowz.rows.length; Y++) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY))
                        continue;
                    RowY rowY = (RowY) nullRowY;
                    for (int y = 0, i1 = 0; y < 16; y++, i1 += 4) {
                        for (int z = 0, i = i1; z < maxz; z += 4, i++) {
                            long bitBuffer = rowY.bits[i];
                            if (bitBuffer != 0) {
                                int lowest = lowestBit(bitBuffer);
                                maxz = z + (lowest >> 4);
                                if (maxz == 0) {
                                    bz = (maxChunkZ << 4);
                                    maxChunkZ--;
                                    break outer;
                                } else {
                                    bz = Math.min(bz, (maxChunkZ << 4) + maxz);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        return bz + getBlockOffsetZ();
    }

    public int getMinX() {
        for (int X = 0; X < rows.length; X++) {
            IRow nullRowX = rows[X];
            if (!(nullRowX instanceof RowX)) continue;
            int bx = X << 4;
            RowX rowx = (RowX) nullRowX;
            long or = 0;
            for (int Z = 0; Z < rowx.rows.length; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ)) continue;
                RowZ rowz = (RowZ) nullRowZ;
                for (int Y = 0; Y < rowz.rows.length; Y++) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY)) continue;
                    RowY rowY = (RowY) nullRowY;
                    or |= Arrays.stream(rowY.bits).reduce(0, (a, b) -> a | b);
                    or = (or & 0xFFFF) | ((or >> 16) & 0xFFFF) | ((or >> 32) & 0xFFFF) | ((or >> 48) & 0xFFFF);
                    if (lowestBit(or) == 0) return bx;
                }
            }
            int lowest = lowestBit(or);
            if (lowest != 64) {
                return bx + lowest + getBlockOffsetX();
            }
        }
        return Integer.MAX_VALUE;
    }

    public void iterate(BlockIterator iterator) {
        for (int X = 0; X < rows.length; X++) {
            IRow nullRowX = rows[X];
            if (!(nullRowX instanceof RowX)) continue;
            int bx = getBlockOffsetX() + (X << 4);
            RowX rowx = (RowX) nullRowX;
            for (int Z = 0; Z < rowx.rows.length; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ)) continue;
                int bz = getBlockOffsetZ() + (Z << 4);
                RowZ rowz = (RowZ) nullRowZ;
                for (int Y = 0; Y < rowz.rows.length; Y++) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY)) continue;
                    int by = Y << 4;
                    RowY rowY = (RowY) nullRowY;
                    for (int y = 0, i = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z += 4, i++) {
                            long bitBuffer = rowY.bits[i];
                            if (bitBuffer != 0) {
                                if (bitBuffer == -1L) {
                                    for (int zz = z; zz < z + 4; zz++) {
                                        for (int x = 0; x < 16; x++) {
                                            iterator.apply(bx + x, by + y, bz + zz);
                                        }
                                    }
                                    continue;
                                }
                                do {
                                    final long lowBit = Long.lowestOneBit(bitBuffer);
                                    final int bitIndex = Long.bitCount(lowBit - 1);
                                    int x = bitIndex & 15;
                                    int zz = z + (bitIndex >> 4);
                                    iterator.apply(bx + x, by + y, bz + zz);
                                    bitBuffer = bitBuffer ^ lowBit;
                                } while (bitBuffer != 0);
                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        return new Iterator<BlockVector3>() {
            private int bx, by, bz, zz, yy;
            private RowX rowX;
            private RowZ rowZ;
            private long[] bits;
            private int bitsIndex = 0;
            private int yIndex = 0;
            private int zIndex = 0;
            private int xIndex = 0;
            private long bitBuffer = 0;
            private boolean next;

            {
                if (nextRowX()) {
                    if (nextRowZ()) {
                        if (nextRowY()) {
                            next = nextLong();
                        }
                    }
                }
            }

            private boolean nextRowX() {
                while (xIndex < rows.length) {
                    bx = getBlockOffsetX() + (xIndex << 4);
                    IRow nullRowX = rows[xIndex++];
                    if (nullRowX instanceof RowX) {
                        rowX = (RowX) nullRowX;
                        return true;
                    }
                }
                return false;
            }

            private boolean nextRowZ() {
                while (zIndex < rowX.rows.length) {
                    bz = getBlockOffsetZ() + (zIndex << 4);
                    IRow nullRowZ = rowX.rows[zIndex++];
                    if (nullRowZ instanceof RowZ) {
                        rowZ = (RowZ) nullRowZ;
                        return true;
                    }
                }
                if (nextRowX()) {
                    zIndex = 0;
                    return nextRowZ();
                }
                return false;
            }

            private boolean nextRowY() {
                while (yIndex < rowZ.rows.length) {
                    by = yIndex << 4;
                    IRow nullRowY = rowZ.rows[yIndex++];
                    if (nullRowY instanceof RowY) {
                        RowY rowY = (RowY) nullRowY;
                        bits = rowY.bits;
                        return true;
                    }
                }
                if (nextRowZ()) {
                    yIndex = 0;
                    return nextRowY();
                }
                return false;
            }

            private boolean nextLong() {
                if (bitBuffer == 0) {
                    do {
                        bitBuffer = bits[bitsIndex++];
                        if (bitsIndex == bits.length) {
                            bitsIndex = 0;
                            if (!nextRowY()) {
                                return next = false;
                            }
                        }
                    } while (bitBuffer == 0);
                    zz = bz + (((bitsIndex - 1) << 2) & 15);
                    yy = by + ((bitsIndex - 1) >> 2);
                }
                return true;
            }

            @Override
            public boolean hasNext() {
                return next;
            }

            @Override
            public BlockVector3 next() {
                final long lowBit = Long.lowestOneBit(bitBuffer);
                final int bitIndex = Long.bitCount(lowBit-1);
                mutable.setComponents((bx) + (bitIndex & 15), yy, (zz) + (bitIndex));
                bitBuffer = bitBuffer ^ lowBit;
                nextLong();
                return mutable;
            }

            @Override
            public void remove() {
                // TODO optimize
                MemBlockSet.this.remove(mutable);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        for (IRow nullRowX : rows) {
            if (!(nullRowX instanceof RowX))
                continue;
            RowX rowx = (RowX) nullRowX;
            for (int Z = 0; Z < rowx.rows.length; Z++) {
                IRow nullRowZ = rowx.rows[Z];
                if (!(nullRowZ instanceof RowZ))
                    continue;
                RowZ rowz = (RowZ) nullRowZ;
                for (int Y = 0; Y < 16; Y++) {
                    IRow nullRowY = rowz.rows[Y];
                    if (!(nullRowY instanceof RowY))
                        continue;
                    RowY rowY = (RowY) nullRowY;
                    for (long bit : rowY.bits) {
                        if (bit != 0)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int size() {
        return (int) sizeLong();
    }

    public long sizeLong() {
        long total = 0;
        long lastBit = 0;
        int lastCount = 0;
        for (IRow nullRowX : rows) {
            if (!(nullRowX instanceof RowX))
                continue;
            RowX rowx = (RowX) nullRowX;
            for (int z = 0; z < rowx.rows.length; z++) {
                IRow nullRowZ = rowx.rows[z];
                if (!(nullRowZ instanceof RowZ))
                    continue;
                RowZ rowz = (RowZ) nullRowZ;
                for (int y = 0; y < 16; y++) {
                    IRow nullRowY = rowz.rows[y];
                    if (!(nullRowY instanceof RowY)) {
                        continue;
                    }
                    RowY rowY = (RowY) nullRowY;
                    for (long bit : rowY.bits) {
                        if (bit == 0) {
                            continue;
                        } else if (bit == -1L) {
                            total += 64;
                        } else if (bit == lastBit) {
                            total += lastCount;
                        } else {
                            lastBit = bit;
                            total += lastCount = Long.bitCount(bit);
                        }
                    }
                }
            }
        }
        return total;
    }

    @Override
    public void clear() {
        Arrays.fill(rows, NULL_ROW_X);
    }

    public interface BlockIterator {
        void apply(int x, int y, int z);
    }

    public interface IRow {
        default boolean get(IRow[] rows, int x, int y, int z) { return false; }
        void set(IRow[] rows, int x, int y, int z);
        default boolean add(IRow[] rows, int x, int y, int z) {
            set(rows, x, y, z);
            return true;
        }
        default boolean remove(IRow[] rows, int x, int y, int z) {
            remove(rows, x, y, z);
            return false;
        }
        default void clear(IRow[] rows, int x, int y, int z) {
        }
    }

    public static final class NullRowX implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowX(parent.length);
            parent[x >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    public static final class NullRowZ implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowZ();
            parent[z >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    public static final class NullRowY implements IRow {
        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            IRow row = new RowY();
            parent[y >> 4] = row;
            row.set(parent, x, y, z);
        }
    }

    public static final class RowX implements IRow {
        private final IRow[] rows;

        public RowX(int size) {
            this.rows = new IRow[size];
            for (int i = 0; i < size; i++) rows[i] = NULL_ROW_Z;
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            return rows[z >> 4].get(this.rows, x, y, z);
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            this.rows[z >> 4].set(this.rows, x, y, z);
        }

        @Override
        public boolean add(IRow[] parent, int x, int y, int z) {
            return this.rows[z >> 4].add(this.rows, x, y, z);
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            this.rows[z >> 4].clear(this.rows, x, y, z);
        }

        @Override
        public boolean remove(IRow[] parent, int x, int y, int z) {
            return this.rows[z >> 4].remove(this.rows, x, y, z);
        }
    }

    public static final class RowZ implements IRow {
        public final IRow[] rows;

        public RowZ() {
            this.rows = new IRow[FaweCache.chunkLayers];
            reset();
        }

        public IRow getRow(int i) {
            return rows[i];
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            return rows[y >> 4].get(this.rows, x, y, z);
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            this.rows[y >> 4].set(this.rows, x, y, z);
        }

        @Override
        public boolean add(IRow[] parent, int x, int y, int z) {
            return this.rows[y >> 4].add(this.rows, x, y, z);
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            this.rows[y >> 4].set(this.rows, x, y, z);
        }

        @Override
        public boolean remove(IRow[] parent, int x, int y, int z) {
            return this.rows[y >> 4].remove(this.rows, x, y, z);
        }

        public boolean isEmpty() {
            return Arrays.stream(rows).noneMatch(row -> row instanceof RowY);
        }

        public void reset(int layer) {
            this.rows[layer] = NULL_ROW_Y;
        }

        public void reset() {
            for (int i = 0; i < FaweCache.chunkLayers; i++) rows[i] = NULL_ROW_Y;
        }
    }

    public static final class RowY implements IRow {
        private final long[] bits;

        public RowY() {
            this.bits = new long[WORDS];
        }

        public long[] getBits() {
            return bits;
        }

        @Override
        public boolean get(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
        }

        @Override
        public void set(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            bits[i >> 6] |= (1L << (i & 0x3F));
        }

        @Override
        public boolean add(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            int offset = i >> 6;
            long value = bits[offset];
            long mask = (1L << (i & 0x3F));
            if ((value & mask) == 0) {
                bits[offset] = value | mask;
                return true;
            }
            return false;
        }

        @Override
        public void clear(IRow[] parent, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            bits[i >> 6] &= ~(1L << (i & 0x3F));
        }

        @Override
        public boolean remove(IRow[] rows, int x, int y, int z) {
            int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            int offset = i >> 6;
            long value = bits[offset];
            long mask = (1L << (i & 0x3F));
            if ((value & mask) != 0) {
                bits[offset] = value & ~mask;
                return true;
            }
            return false;
        }
    }

    private static IRow[] resize(IRow[] arr, IRow def) {
        int len = arr.length;
        int newLen = len == 1 ? 1 : Integer.highestOneBit(len - 1) * 2;
        IRow[] copy = Arrays.copyOf(arr, newLen, IRow[].class);
        for (int i = len; i < newLen; i++) copy[i] = def;
        return copy;
    }
}
