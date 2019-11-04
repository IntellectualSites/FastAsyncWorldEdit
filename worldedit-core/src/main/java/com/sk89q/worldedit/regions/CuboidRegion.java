/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * An axis-aligned cuboid. It can be defined using two corners of the cuboid.
 */
public class CuboidRegion extends AbstractRegion implements FlatRegion {


    private boolean useOldIterator;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private BlockVector3 pos1;
    private BlockVector3 pos2;

    /**
     * Construct a new instance of this cuboid using two corners of the cuboid.
     *
     * @param pos1 the first position
     * @param pos2 the second position
     */
    public CuboidRegion(BlockVector3 pos1, BlockVector3 pos2) {
        this(null, pos1, pos2);
    }

    /**
     * Construct a new instance of this cuboid using two corners of the cuboid.
     *
     * @param world the world
     * @param pos1  the first position
     * @param pos2  the second position
     */
    public CuboidRegion(World world, BlockVector3 pos1, BlockVector3 pos2) {
        super(world);
        checkNotNull(pos1);
        checkNotNull(pos2);
        this.pos1 = pos1;
        this.pos2 = pos2;
        recalculate();
    }

    public void setUseOldIterator(boolean useOldIterator) {
        this.useOldIterator = useOldIterator;
    }

    /**
     * Get the first cuboid-defining corner.
     *
     * @return a position
     */
    public BlockVector3 getPos1() {
        return pos1;
    }

    /**
     * Set the first cuboid-defining corner.
     *
     * @param pos1 a position
     */
    public void setPos1(BlockVector3 pos1) {
        this.pos1 = pos1;
        recalculate();
    }

    /**
     * Get the second cuboid-defining corner.
     *
     * @return a position
     */
    public BlockVector3 getPos2() {
        return pos2;
    }

    /**
     * Set the second cuboid-defining corner.
     *
     * @param pos2 a position
     */
    public void setPos2(BlockVector3 pos2) {
        this.pos2 = pos2;
        recalculate();
    }

    /**
     * Clamps the cuboid according to boundaries of the world.
     */
    protected void recalculate() {
        if (pos1 == null || pos2 == null) {
            return;
        }
        pos1 = pos1.clampY(world == null ? Integer.MIN_VALUE : 0, world == null ? Integer.MAX_VALUE : world.getMaxY());
        pos2 = pos2.clampY(world == null ? Integer.MIN_VALUE : 0, world == null ? Integer.MAX_VALUE : world.getMaxY());
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        minX = min.getBlockX();
        minY = min.getBlockY();
        minZ = min.getBlockZ();
        maxX = max.getBlockX();
        maxY = max.getBlockY();
        maxZ = max.getBlockZ();
    }

    /**
     * Get a region that contains the faces of this cuboid.
     *
     * @return a new complex region
     */
    public Region getFaces() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return new RegionIntersection(
                // Project to Z-Y plane
                new CuboidRegion(pos1.withX(min.getX()), pos2.withX(min.getX())),
                new CuboidRegion(pos1.withX(max.getX()), pos2.withX(max.getX())),

                // Project to X-Y plane
                new CuboidRegion(pos1.withZ(min.getZ()), pos2.withZ(min.getZ())),
                new CuboidRegion(pos1.withZ(max.getZ()), pos2.withZ(max.getZ())),

                // Project to the X-Z plane
                new CuboidRegion(pos1.withY(min.getY()), pos2.withY(min.getY())),
                new CuboidRegion(pos1.withY(max.getY()), pos2.withY(max.getY())));
    }

    /**
     * Get a region that contains the walls (all faces but the ones parallel to
     * the X-Z plane) of this cuboid.
     *
     * @return a new complex region
     */
    public Region getWalls() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return new RegionIntersection(
                // Project to Z-Y plane
                new CuboidRegion(pos1.withX(min.getX()), pos2.withX(min.getX())),
                new CuboidRegion(pos1.withX(max.getX()), pos2.withX(max.getX())),

                // Project to X-Y plane
                new CuboidRegion(pos1.withZ(min.getZ()), pos2.withZ(min.getZ())),
                new CuboidRegion(pos1.withZ(max.getZ()), pos2.withZ(max.getZ())));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return pos1.getMinimum(pos2);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return pos1.getMaximum(pos2);
    }

    @Override
    public int getMinimumY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }

    @Override
    public int getMaximumY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    @Override
    public void expand(BlockVector3... changes) {
        checkNotNull(changes);

        for (BlockVector3 change : changes) {
            if (change.getX() > 0) {
                if (Math.max(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(change.getX(), 0, 0);
                } else {
                    pos2 = pos2.add(change.getX(), 0, 0);
                }
            } else {
                if (Math.min(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(change.getX(), 0, 0);
                } else {
                    pos2 = pos2.add(change.getX(), 0, 0);
                }
            }

            if (change.getY() > 0) {
                if (Math.max(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(0, change.getY(), 0);
                } else {
                    pos2 = pos2.add(0, change.getY(), 0);
                }
            } else {
                if (Math.min(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(0, change.getY(), 0);
                } else {
                    pos2 = pos2.add(0, change.getY(), 0);
                }
            }

            if (change.getZ() > 0) {
                if (Math.max(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(0, 0, change.getZ());
                } else {
                    pos2 = pos2.add(0, 0, change.getZ());
                }
            } else {
                if (Math.min(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(0, 0, change.getZ());
                } else {
                    pos2 = pos2.add(0, 0, change.getZ());
                }
            }
        }

        recalculate();
    }

    @Override
    public void contract(BlockVector3... changes) {
        checkNotNull(changes);

        for (BlockVector3 change : changes) {
            if (change.getX() < 0) {
                if (Math.max(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(change.getX(), 0, 0);
                } else {
                    pos2 = pos2.add(change.getX(), 0, 0);
                }
            } else {
                if (Math.min(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(change.getX(), 0, 0);
                } else {
                    pos2 = pos2.add(change.getX(), 0, 0);
                }
            }

            if (change.getY() < 0) {
                if (Math.max(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(0, change.getY(), 0);
                } else {
                    pos2 = pos2.add(0, change.getY(), 0);
                }
            } else {
                if (Math.min(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(0, change.getY(), 0);
                } else {
                    pos2 = pos2.add(0, change.getY(), 0);
                }
            }

            if (change.getZ() < 0) {
                if (Math.max(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(0, 0, change.getZ());
                } else {
                    pos2 = pos2.add(0, 0, change.getZ());
                }
            } else {
                if (Math.min(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(0, 0, change.getZ());
                } else {
                    pos2 = pos2.add(0, 0, change.getZ());
                }
            }
        }

        recalculate();
    }

    @Override
    public void shift(BlockVector3 change) throws RegionOperationException {
        pos1 = pos1.add(change);
        pos2 = pos2.add(change);

        recalculate();
    }

    @Override
    public Set<BlockVector2> getChunks() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();
        final int maxX = max.getBlockX() >> ChunkStore.CHUNK_SHIFTS;
        final int minX = min.getBlockX() >> ChunkStore.CHUNK_SHIFTS;
        final int maxZ = max.getBlockZ() >> ChunkStore.CHUNK_SHIFTS;
        final int minZ = min.getBlockZ() >> ChunkStore.CHUNK_SHIFTS;
        final int size = (maxX - minX + 1) * (maxZ - minZ + 1);

        return new AbstractSet<BlockVector2>() {
            @NotNull @Override
            public Iterator<BlockVector2> iterator() {
                return new Iterator<BlockVector2>() {
                    final MutableBlockVector2 mutable = new MutableBlockVector2(0, 0);

                    int bx = minX;
                    int bz = minZ;

                    int tx = maxX;
                    int tz = maxZ;

                    private int x = minX;
                    private int z = minZ;

                    int regionX = x >> 5;
                    int regionZ = z >> 5;
                    int rbx = Math.max(bx, regionX << 5);
                    int rbz = Math.max(bz, regionZ << 5);
                    int rtx = Math.min(tx, 31 + (regionX << 5));
                    int rtz = Math.min(tz, 31 + (regionZ << 5));

                    boolean hasNext = true;


                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public BlockVector2 next() {
                        mutable.mutX(x);
                        mutable.mutZ(z);
                        if (++x > rtx) {
                            if (++z > rtz) {
                                if (x > tx) {
                                    x = bx;
                                    if (z > tz) {
                                        if (!hasNext) {
                                            throw new NoSuchElementException("End of iterator") {
                                                @Override
                                                public Throwable fillInStackTrace() {
                                                    return this;
                                                }
                                            };
                                        }
                                        x = tx;
                                        hasNext = false;
                                        return mutable;
                                    }
                                } else {
                                    z = rbz;
                                }
                                regionX = x >> 5;
                                regionZ = z >> 5;
                                rbx = Math.max(bx, regionX << 5);
                                rbz = Math.max(bz, regionZ << 5);
                                rtx = Math.min(tx, 31 + (regionX << 5));
                                rtz = Math.min(tz, 31 + (regionZ << 5));
                            } else {
                                x = rbx;
                            }
                        }
                        return mutable;
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }


            @Override
            public boolean contains(Object o) {
                if (o instanceof BlockVector2) {
                    BlockVector2 cv = (BlockVector2) o;
                    return cv.getX() >= minX && cv.getX() <= maxX && cv.getZ() >= minZ && cv.getZ() <= maxZ;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    public Set<BlockVector3> getChunkCubes() {
        Set<BlockVector3> chunks = new BlockVectorSet();

        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        for (int x = min.getBlockX() >> ChunkStore.CHUNK_SHIFTS; x <= max.getBlockX() >> ChunkStore.CHUNK_SHIFTS; ++x) {
            for (int z = min.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; z <= max.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; ++z) {
                for (int y = min.getBlockY() >> ChunkStore.CHUNK_SHIFTS; y <= max.getBlockY() >> ChunkStore.CHUNK_SHIFTS; ++y) {
                    chunks.add(BlockVector3.at(x, y, z));
                }
            }
        }

        return chunks;
    }

    /* Slow and unnecessary
    @Override
    public boolean contains(BlockVector3 position) {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return position.containedWithin(min, max);
    }
    */

    @Override
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    @Override
    public boolean contains(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    @Override
    public boolean contains(BlockVector3 position) {
        return contains(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        if (Settings.IMP.HISTORY.COMPRESSION_LEVEL >= 9 || useOldIterator) {
            return iterator_old();
        }
        return new Iterator<BlockVector3>() {
            final MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
            private BlockVector3 min = getMinimumPoint();
            private BlockVector3 max = getMaximumPoint();

            int bx = min.getBlockX();
            int by = min.getBlockY();
            int bz = min.getBlockZ();

            int tx = max.getBlockX();
            int ty = max.getBlockY();
            int tz = max.getBlockZ();

            private int x = min.getBlockX();
            private int y = min.getBlockY();
            private int z = min.getBlockZ();

            int cx = x >> 4;
            int cz = z >> 4;
            int cbx = Math.max(bx, cx << 4);
            int cbz = Math.max(bz, cz << 4);
            int ctx = Math.min(tx, 15 + (cx << 4));
            int ctz = Math.min(tz, 15 + (cz << 4));

            boolean hasNext = true;


            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public BlockVector3 next() {
                mutable.mutX(x);
                mutable.mutY(y);
                mutable.mutZ(z);
                if (++x > ctx) {
                    if (++z > ctz) {
                        if (++y > ty) {
                            y = by;
                            if (x > tx) {
                                x = bx;
                                if (z > tz) {
                                    if (!hasNext) {
                                        throw new NoSuchElementException("End of iterator") {
                                            @Override
                                            public Throwable fillInStackTrace() {
                                                return this;
                                            }
                                        };
                                    }
                                    x = tx;
                                    y = ty;
                                    hasNext = false;
                                    return mutable;
                                }
                            } else {
                                z = cbz;
                            }
                            cx = x >> 4;
                            cz = z >> 4;
                            cbx = Math.max(bx, cx << 4);
                            cbz = Math.max(bz, cz << 4);
                            ctx = Math.min(tx, 15 + (cx << 4));
                            ctz = Math.min(tz, 15 + (cz << 4));
                        } else {
                            x = cbx;
                            z = cbz;
                        }
                    } else {
                        x = cbx;
                    }
                }
                return mutable;
            }
        };
    }

    public Iterator<BlockVector3> iterator_old() {
        final MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
        return new Iterator<BlockVector3>() {
            private BlockVector3 min = getMinimumPoint();
            private BlockVector3 max = getMaximumPoint();
            private int nextX = min.getBlockX();
            private int nextY = min.getBlockY();
            private int nextZ = min.getBlockZ();
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public BlockVector3 next() {
                mutable.mutX(nextX);
                mutable.mutY(nextY);
                mutable.mutZ(nextZ);
                if (++nextX > max.getBlockX()) {
                    nextX = min.getBlockX();
                    if (++nextZ > max.getBlockZ()) {
                        nextZ = min.getBlockZ();
                        if (++nextY > max.getBlockY()) {
                            if (!hasNext()) throw new NoSuchElementException();
                            nextX = max.getBlockX();
                            nextZ = max.getBlockZ();
                            nextY = max.getBlockY();
                            hasNext = false;
                        }
                    }
                }
                return mutable;
            }
        };
    }

    @Override
    public Iterable<BlockVector2> asFlatRegion() {
        return () -> new Iterator<BlockVector2>() {
            private BlockVector3 min = getMinimumPoint();
            private BlockVector3 max = getMaximumPoint();
            private int nextX = min.getBlockX();
            private int nextZ = min.getBlockZ();

            @Override
            public boolean hasNext() {
                return (nextZ != Integer.MAX_VALUE);
            }

            @Override
            public BlockVector2 next() {
                if (!hasNext()) throw new NoSuchElementException();
                BlockVector2 answer = BlockVector2.at(nextX, nextZ);
                if (++nextX > max.getBlockX()) {
                    nextX = min.getBlockX();
                    if (++nextZ > max.getBlockZ()) {
                        nextZ = Integer.MAX_VALUE;
                        nextX = Integer.MAX_VALUE;
                    }
                }
                return answer;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        return getMinimumPoint() + " - " + getMaximumPoint();
    }

    @Override
    public CuboidRegion clone() {
        return (CuboidRegion) super.clone();
    }

    /**
     * Make a cuboid region out of the given region using the minimum and maximum
     * bounds of the provided region.
     *
     * @param region the region
     * @return a new cuboid region
     */
    public static CuboidRegion makeCuboid(Region region) {
        checkNotNull(region);
        return new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
    }

    public static boolean contains(CuboidRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return region.contains(min.getBlockX(), min.getBlockY(), min.getBlockZ()) && region.contains(max.getBlockX(), max.getBlockY(), max.getBlockZ());
    }

    /**
     * Make a cuboid from the center.
     *
     * @param origin the origin
     * @param apothem the apothem, where 0 is the minimum value to make a 1x1 cuboid
     * @return a cuboid region
     */
    public static CuboidRegion fromCenter(BlockVector3 origin, int apothem) {
        checkNotNull(origin);
        checkArgument(apothem >= 0, "apothem => 0 required");
        BlockVector3 size = BlockVector3.ONE.multiply(apothem);
        return new CuboidRegion(origin.subtract(size), origin.add(size));
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public void filter(final IChunk chunk, final Filter filter, ChunkFilterBlock block, final IChunkGet get, final IChunkSet set) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        block = block.init(chunkX, chunkZ, get);


        if ((minX + 15) >> 4 <= chunkX && (maxX - 15) >> 4 >= chunkX && (minZ + 15) >> 4 <= chunkZ && (maxZ - 15) >> 4 >= chunkZ) {
            filter(chunk, filter, block, get, set, minY, maxY);
            return;
        }
        int localMinX = Math.max(minX, chunkX << 4) & 15;
        int localMaxX = Math.min(maxX, 15 + (chunkX << 4)) & 15;
        int localMinZ = Math.max(minZ, chunkZ << 4) & 15;
        int localMaxZ = Math.min(maxZ, 15 + (chunkZ << 4)) & 15;

        int yStart = (minY & 15);
        int yEnd = (maxY & 15);

        int minSection = minY >> 4;
        int maxSection = maxY >> 4;
        if (minSection == maxSection) {
            filter(chunk, filter, block, get, set, minSection, localMinX, yStart, localMinZ, localMaxX, yEnd, localMaxZ);
            return;
        }
        if (yStart != 0) {
            filter(chunk, filter, block, get, set, minSection, localMinX, yStart, localMinZ, localMaxX, 15, localMaxZ);
            minSection++;
        }
        if (yEnd != 15) {
            filter(chunk, filter, block, get, set, minSection, localMinX, 0, localMinZ, localMaxX, 15, localMaxZ);
            maxSection--;
        }
        for (int layer = minSection; layer <= maxSection; layer++) {
            filter(chunk, filter, block, get, set, layer, localMinX, yStart, localMinZ, localMaxX, yEnd, localMaxZ);
        }
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        if (bx >= minX && tx <= maxX && bz >= minZ && tz <= maxZ) {
            // contains all X/Z
            if (minY <= 0 && maxY >= 255) {
                return set;
            }
            trimY(set, minY, maxY);
            trimNBT(set, this::contains);
            return set;
        }
        else if (tx >= minX && bx <= maxX && tz >= minZ && bz <= maxZ) {
            trimY(set, minY, maxY);
            final int lowerX = Math.max(0, minX - bx);
            final int upperX = Math.min(15, 15 + maxX - tx);

            final int lowerZ = Math.max(0, minZ - bz);
            final int upperZ = Math.min(15, 15 + maxZ - tz);

            final int upperZi = ((upperZ + 1) << 4);
            final int lowerZi = (lowerZ << 4);

            boolean trimX = lowerX != 0 || upperX != 15;
            boolean trimZ = lowerZ != 0 || upperZ != 15;

            int indexY, index;
            for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
                if (set.hasSection(layer)) {
                    char[] arr = set.getArray(layer);
                    if (trimX || trimZ) {
                        indexY = 0;
                        for (int y = 0; y < 16; y++, indexY += 256) {
                            if (trimZ) {
                                index = indexY;
                                for (int z = 0; z < lowerZ; z++) {
                                    // null the z values
                                    for (int x = 0; x < 16; x++, index++) {
                                        arr[index] = 0;
                                    }
                                }
                                index = indexY + upperZi;
                                for (int z = upperZ + 1; z < 16; z++) {
                                    // null the z values
                                    for (int x = 0; x < 16; x++, index++) {
                                        arr[index] = 0;
                                    }
                                }
                            }
                            if (trimX) {
                                index = indexY + lowerZi;
                                for (int z = lowerZ; z <= upperZ; z++, index += 16) {
                                    for (int x = 0; x < lowerX; x++) {
                                        // null the x values
                                        arr[index + x] = 0;
                                    }
                                    for (int x = upperX + 1; x < 16; x++) {
                                        // null the x values
                                        arr[index + x] = 0;
                                    }
                                }
                            }
                        }
                        set.setBlocks(layer, arr);
                    }
                }
            }
            trimNBT(set, this::contains);
            return set;
        } else {
            return null;
        }
    }


}
