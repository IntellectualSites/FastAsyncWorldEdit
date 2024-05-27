/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An axis-aligned cuboid. It can be defined using two corners of the cuboid.
 */
public class CuboidRegion extends AbstractRegion implements FlatRegion {

    //FAWE start
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    //FAWE end
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

    //FAWE start - allow region to be created without clamping Y
    /**
     * Construct a new instance of this cuboid using two corners of the cuboid.
     *
     * @param world  the world
     * @param pos1   the first position
     * @param pos2   the second position
     * @param clampY if the min/max Y of the region should be clamped to the world
     * @since 2.1.0
     */
    public CuboidRegion(World world, BlockVector3 pos1, BlockVector3 pos2, boolean clampY) {
        super(world);
        checkNotNull(pos1);
        checkNotNull(pos2);
        this.pos1 = pos1;
        this.pos2 = pos2;
        if (clampY) {
            recalculate();
        } else {
            recalculateNoClamp();
        }
    }
    //FAWE end

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
        //FAWE start
        recalculate();
        //FAWE end
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
        //FAWE start
        recalculate();
        //FAWE end
    }

    /**
     * Sets the cached min and max x/y/z and clamps Y to world y min/max
     */
    protected void recalculate() {
        if (pos1 == null || pos2 == null) {
            return;
        }
        pos1 = pos1.clampY(getWorldMinY(), getWorldMaxY());
        pos2 = pos2.clampY(getWorldMinY(), getWorldMaxY());
        minX = Math.min(pos1.x(), pos2.x());
        minY = Math.min(pos1.y(), pos2.y());
        minZ = Math.min(pos1.z(), pos2.z());
        maxX = Math.max(pos1.x(), pos2.x());
        maxY = Math.max(pos1.y(), pos2.y());
        maxZ = Math.max(pos1.z(), pos2.z());
    }

    //FAWE start - allow region to be created without clamping Y
    /**
     * Sets the cached min and max x/y/z
     */
    protected void recalculateNoClamp() {
        if (pos1 == null || pos2 == null) {
            return;
        }
        minX = Math.min(pos1.x(), pos2.x());
        minY = Math.min(pos1.y(), pos2.y());
        minZ = Math.min(pos1.z(), pos2.z());
        maxX = Math.max(pos1.x(), pos2.x());
        maxY = Math.max(pos1.y(), pos2.y());
        maxZ = Math.max(pos1.z(), pos2.z());
    }
    //FAWE end

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
                new CuboidRegion(pos1.withX(min.x()), pos2.withX(min.x())),
                new CuboidRegion(pos1.withX(max.x()), pos2.withX(max.x())),

                // Project to X-Y plane
                new CuboidRegion(pos1.withZ(min.z()), pos2.withZ(min.z())),
                new CuboidRegion(pos1.withZ(max.z()), pos2.withZ(max.z())),

                // Project to the X-Z plane
                new CuboidRegion(pos1.withY(min.y()), pos2.withY(min.y())),
                new CuboidRegion(pos1.withY(max.y()), pos2.withY(max.y()))
        );
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
        BlockVector3 dimensions = getDimensions();

        //FAWE start
        if (dimensions.x() <= 2 || dimensions.z() <= 2) {
            // The wall are the region
            return new RegionIntersection(this);
        }
        //FAWE end

        return new RegionIntersection(
                // Project to Z-Y plane
                new CuboidRegion(pos1.withX(min.x()), pos2.withX(min.x())),
                new CuboidRegion(pos1.withX(max.x()), pos2.withX(max.x())),

                // Project to X-Y plane
                //FAWE start = prevent overlap
                new CuboidRegion(
                        pos1.withZ(min.z()).add(BlockVector3.UNIT_X),
                        pos2.withZ(min.z()).subtract(BlockVector3.UNIT_X)
                ),
                new CuboidRegion(
                        pos1.withZ(max.z()).add(BlockVector3.UNIT_X),
                        pos2.withZ(max.z()).subtract(BlockVector3.UNIT_X)
                )
                //FAWE end
        );
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
    public CuboidRegion getBoundingBox() {
        return this;
    }

    @Override
    public int getMinimumY() {
        return minY;
    }

    @Override
    public int getMaximumY() {
        return maxY;
    }

    @Override
    public void expand(BlockVector3... changes) {
        checkNotNull(changes);

        for (BlockVector3 change : changes) {
            if (change.x() > 0) {
                if (Math.max(pos1.x(), pos2.x()) == pos1.x()) {
                    pos1 = pos1.add(change.x(), 0, 0);
                } else {
                    pos2 = pos2.add(change.x(), 0, 0);
                }
            } else {
                if (Math.min(pos1.x(), pos2.x()) == pos1.x()) {
                    pos1 = pos1.add(change.x(), 0, 0);
                } else {
                    pos2 = pos2.add(change.x(), 0, 0);
                }
            }

            if (change.y() > 0) {
                if (Math.max(pos1.y(), pos2.y()) == pos1.y()) {
                    pos1 = pos1.add(0, change.y(), 0);
                } else {
                    pos2 = pos2.add(0, change.y(), 0);
                }
            } else {
                if (Math.min(pos1.y(), pos2.y()) == pos1.y()) {
                    pos1 = pos1.add(0, change.y(), 0);
                } else {
                    pos2 = pos2.add(0, change.y(), 0);
                }
            }

            if (change.z() > 0) {
                if (Math.max(pos1.z(), pos2.z()) == pos1.z()) {
                    pos1 = pos1.add(0, 0, change.z());
                } else {
                    pos2 = pos2.add(0, 0, change.z());
                }
            } else {
                if (Math.min(pos1.z(), pos2.z()) == pos1.z()) {
                    pos1 = pos1.add(0, 0, change.z());
                } else {
                    pos2 = pos2.add(0, 0, change.z());
                }
            }
        }

        recalculate();
    }

    @Override
    public void contract(BlockVector3... changes) {
        checkNotNull(changes);

        for (BlockVector3 change : changes) {
            if (change.x() < 0) {
                if (Math.max(pos1.x(), pos2.x()) == pos1.x()) {
                    pos1 = pos1.add(change.x(), 0, 0);
                } else {
                    pos2 = pos2.add(change.x(), 0, 0);
                }
            } else {
                if (Math.min(pos1.x(), pos2.x()) == pos1.x()) {
                    pos1 = pos1.add(change.x(), 0, 0);
                } else {
                    pos2 = pos2.add(change.x(), 0, 0);
                }
            }

            if (change.y() < 0) {
                if (Math.max(pos1.y(), pos2.y()) == pos1.y()) {
                    pos1 = pos1.add(0, change.y(), 0);
                } else {
                    pos2 = pos2.add(0, change.y(), 0);
                }
            } else {
                if (Math.min(pos1.y(), pos2.y()) == pos1.y()) {
                    pos1 = pos1.add(0, change.y(), 0);
                } else {
                    pos2 = pos2.add(0, change.y(), 0);
                }
            }

            if (change.z() < 0) {
                if (Math.max(pos1.z(), pos2.z()) == pos1.z()) {
                    pos1 = pos1.add(0, 0, change.z());
                } else {
                    pos2 = pos2.add(0, 0, change.z());
                }
            } else {
                if (Math.min(pos1.z(), pos2.z()) == pos1.z()) {
                    pos1 = pos1.add(0, 0, change.z());
                } else {
                    pos2 = pos2.add(0, 0, change.z());
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
        final int maxX = max.x() >> ChunkStore.CHUNK_SHIFTS;
        final int minX = min.x() >> ChunkStore.CHUNK_SHIFTS;
        final int maxZ = max.z() >> ChunkStore.CHUNK_SHIFTS;
        final int minZ = min.z() >> ChunkStore.CHUNK_SHIFTS;
        final int size = (maxX - minX + 1) * (maxZ - minZ + 1);

        //FAWE start
        return new AbstractSet<>() {
            @Nonnull
            @Override
            public Iterator<BlockVector2> iterator() {
                return new Iterator<>() {

                    final int bx = minX;
                    final int bz = minZ;

                    final int tx = maxX;
                    final int tz = maxZ;

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
                        int curX = x;
                        int curZ = z;
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
                                        return BlockVector2.at(curX, curZ);
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
                        return BlockVector2.at(curX, curZ);
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
                    return cv.x() >= minX && cv.x() <= maxX && cv.z() >= minZ && cv.z() <= maxZ;
                }
                return false;
            }
        };
    }
    //FAWE end

    @Override
    public Set<BlockVector3> getChunkCubes() {
        //FAWE start - BlockVectorSet instead of HashMap
        Set<BlockVector3> chunks = new BlockVectorSet();
        //FAWE end

        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        for (int x = min.x() >> ChunkStore.CHUNK_SHIFTS; x <= max.x() >> ChunkStore.CHUNK_SHIFTS; ++x) {
            for (int z = min.z() >> ChunkStore.CHUNK_SHIFTS; z <= max.z() >> ChunkStore.CHUNK_SHIFTS; ++z) {
                for (int y = min.y() >> ChunkStore.CHUNK_SHIFTS; y <= max.y() >> ChunkStore.CHUNK_SHIFTS; ++y) {
                    chunks.add(BlockVector3.at(x, y, z));
                }
            }
        }

        return chunks;
    }


    @Override
    public boolean contains(BlockVector3 position) {
        //FAWE start
        return contains(position.x(), position.y(), position.z());
        //FAWE end
    }

    //FAWE start
    @Override
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    @Override
    public boolean contains(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }
    //FAWE end

    @Override
    public Iterator<BlockVector3> iterator() {
        //FAWE start
        if (Settings.settings().HISTORY.COMPRESSION_LEVEL >= 9) {
            return iterator_old();
        }
        //FAWE end
        return new Iterator<BlockVector3>() {
            //FAWE start
            final MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
            private final BlockVector3 min = getMinimumPoint();
            private final BlockVector3 max = getMaximumPoint();

            final int bx = min.x();
            final int by = min.y();
            final int bz = min.z();

            final int tx = max.x();
            final int ty = max.y();
            final int tz = max.z();

            private int x = min.x();
            private int y = min.y();
            private int z = min.z();

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
            private final BlockVector3 min = getMinimumPoint();
            private final BlockVector3 max = getMaximumPoint();
            private int nextX = min.x();
            private int nextY = min.y();
            private int nextZ = min.z();
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return (hasNext);
            }

            @Override
            public BlockVector3 next() {
                mutable.mutX(nextX);
                mutable.mutY(nextY);
                mutable.mutZ(nextZ);
                if (++nextX > max.x()) {
                    nextX = min.x();
                    if (++nextZ > max.z()) {
                        nextZ = min.z();
                        if (++nextY > max.y()) {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            nextX = max.x();
                            nextZ = max.z();
                            nextY = max.y();
                            hasNext = false;
                        }
                    }
                }
                return mutable;
            }
        };
    }
    //FAWE end

    @Override
    public Iterable<BlockVector2> asFlatRegion() {
        return () -> new Iterator<BlockVector2>() {
            private final BlockVector3 min = getMinimumPoint();
            private final BlockVector3 max = getMaximumPoint();
            private int nextX = min.x();
            private int nextZ = min.z();

            @Override
            public boolean hasNext() {
                return (nextZ != Integer.MAX_VALUE);
            }

            @Override
            public BlockVector2 next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                BlockVector2 answer = BlockVector2.at(nextX, nextZ);
                if (++nextX > max.x()) {
                    nextX = min.x();
                    if (++nextZ > max.z()) {
                        nextZ = Integer.MAX_VALUE;
                        nextX = Integer.MAX_VALUE;
                    }
                }
                return answer;
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

    //FAWE start
    public static boolean contains(CuboidRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return region.contains(min.x(), min.y(), min.z()) && region.contains(
                max.x(),
                max.y(),
                max.z()
        );
    }
    //FAWE end

    /**
     * Make a cuboid from the center.
     *
     * @param origin  the origin
     * @param apothem the apothem, where 0 is the minimum value to make a 1x1 cuboid
     * @return a cuboid region
     */
    public static CuboidRegion fromCenter(BlockVector3 origin, int apothem) {
        checkNotNull(origin);
        checkArgument(apothem >= 0, "apothem => 0 required");
        BlockVector3 size = BlockVector3.ONE.multiply(apothem);
        return new CuboidRegion(origin.subtract(size), origin.add(size));
    }

    //FAWE start
    public int getMinimumX() {
        return minX;
    }

    public int getMinimumZ() {
        return minZ;
    }

    public int getMaximumX() {
        return maxX;
    }

    public int getMaximumZ() {
        return maxZ;
    }

    @Override
    public void filter(
            final IChunk chunk,
            final Filter filter,
            ChunkFilterBlock block,
            final IChunkGet get,
            final IChunkSet set,
            boolean full
    ) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        block = block.initChunk(chunkX, chunkZ);

        //Chunk entry is an "interior chunk" in regards to the entire region, so filter the chunk whole instead of partially
        if ((minX + 15) >> 4 <= chunkX && (maxX - 15) >> 4 >= chunkX && (minZ + 15) >> 4 <= chunkZ && (maxZ - 15) >> 4 >= chunkZ) {
            filter(chunk, filter, block, get, set, minY, maxY, full);
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
            filter(chunk, filter, block, get, set, minSection, localMinX, yStart, localMinZ, localMaxX, yEnd, localMaxZ, full);
            return;
        }
        //If the yStart is not 0, the edit is smaller than the height of a ChunkSection, so filter individually and remove section as the minSection layer entry
        if (yStart != 0) {
            filter(chunk, filter, block, get, set, minSection, localMinX, yStart, localMinZ, localMaxX, 15, localMaxZ, full);
            minSection++;
        }
        //If the yEnd is not 15, the edit is smaller than the height of a ChunkSection, so filter individually and remove section as the maxSection layer entry
        if (yEnd != 15) {
            filter(chunk, filter, block, get, set, maxSection, localMinX, 0, localMinZ, localMaxX, yEnd, localMaxZ, full);
            maxSection--;
        }
        for (int layer = minSection; layer <= maxSection; layer++) {
            filter(chunk, filter, block, get, set, layer, localMinX, 0, localMinZ, localMaxX, 15, localMaxZ, full);
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
            if (minY <= set.getMinSectionPosition() << 4 && maxY >= (set.getMaxSectionPosition() << 4) + 15) {
                return set;
            }
            trimY(set, minY, maxY, true);
            BlockVector3 chunkPos = chunk.getChunkBlockCoord().withY(0);
            trimNBT(set, this::contains, pos -> this.contains(pos.add(chunkPos)));
            return set;
        }
        if (tx >= minX && bx <= maxX && tz >= minZ && bz <= maxZ) {
            if (minY > set.getMinSectionPosition() << 4 || maxY < (set.getMaxSectionPosition() << 4) + 15) {
                trimY(set, minY, maxY, true);
            }
            final int lowerX = Math.max(0, minX - bx);
            final int upperX = Math.min(15, 15 + maxX - tx);

            final int lowerZ = Math.max(0, minZ - bz);
            final int upperZ = Math.min(15, 15 + maxZ - tz);

            final int upperZi = ((upperZ + 1) << 4);
            final int lowerZi = (lowerZ << 4);

            boolean trimX = lowerX != 0 || upperX != 15;
            boolean trimZ = lowerZ != 0 || upperZ != 15;

            if (!(trimX || trimZ)) {
                return set;
            }

            for (int layer = get.getMinSectionPosition(); layer < get.getMaxSectionPosition(); layer++) {
                if (!set.hasSection(layer)) {
                    continue;
                }
                char[] arr = Objects.requireNonNull(set.loadIfPresent(layer)); // This shouldn't be null if above is true
                int indexY = 0;
                for (int y = 0; y < 16; y++, indexY += 256) { // For each y layer within a chunk section
                    int index;
                    if (trimZ) {
                        index = indexY;
                        for (int z = 0; z < lowerZ; z++) {
                            // null the z values
                            for (int x = 0; x < 16; x++, index++) {
                                arr[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                        }
                        index = indexY + upperZi;
                        for (int z = upperZ + 1; z < 16; z++) {
                            // null the z values
                            for (int x = 0; x < 16; x++, index++) {
                                arr[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                        }
                    }
                    if (trimX) {
                        index = indexY + lowerZi; // Skip blocks already removed by trimZ
                        for (int z = lowerZ; z <= upperZ; z++, index += 16) {
                            for (int x = 0; x < lowerX; x++) {
                                // null the x values
                                arr[index + x] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                            for (int x = upperX + 1; x < 16; x++) {
                                // null the x values
                                arr[index + x] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                        }
                    }
                }
                set.setBlocks(layer, arr);
            }
            final BlockVector3 chunkPos = BlockVector3.at(chunk.getX() << 4, 0, chunk.getZ() << 4);
            trimNBT(set, this::contains, pos -> this.contains(pos.add(chunkPos)));
            return set;
        }
        return null;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set, boolean asBlacklist) {
        if (!asBlacklist) {
            return processSet(chunk, get, set);
        }
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;

        if (bx >= minX && tx <= maxX && bz >= minZ && tz <= maxZ) {
            // contains all X/Z
            int sMaxY = (set.getMaxSectionPosition() << 4) + 15;
            int sMinY = set.getMinSectionPosition() << 4;
            if (minY <= sMinY && maxY >= sMaxY) {
                return null;
            }
            trimY(set, minY, maxY, false);
            BlockVector3 chunkPos = chunk.getChunkBlockCoord().withY(0);
            trimNBT(set, this::contains, pos -> this.contains(pos.add(chunkPos)));
            return set;
        }
        if (tx >= minX && bx <= maxX && tz >= minZ && bz <= maxZ) {
            if (minY > set.getMinSectionPosition() << 4 || maxY < (set.getMaxSectionPosition() << 4) + 15) {
                trimY(set, minY, maxY, false);
            }
            final int lowerX = Math.max(0, minX - bx);
            final int upperX = Math.min(15, 15 + maxX - tx);

            final int lowerZ = Math.max(0, minZ - bz);
            final int upperZ = Math.min(15, 15 + maxZ - tz);

            final int lowerZi = (lowerZ << 4);

            boolean trimX = lowerX != 0 || upperX != 15;
            boolean trimZ = lowerZ != 0 || upperZ != 15;

            for (int layer = get.getMinSectionPosition(); layer < get.getMaxSectionPosition(); layer++) {
                if (!set.hasSection(layer)) {
                    continue;
                }
                char[] arr = Objects.requireNonNull(set.loadIfPresent(layer)); // This shouldn't be null if above is true
                if (!(trimX || trimZ)) {
                    continue;
                }
                int indexY = 0;
                for (int y = 0; y < 16; y++, indexY += 256) { // For each y layer within a chunk section
                    int index;
                    if (trimZ) {
                        index = indexY;
                        for (int z = lowerZ; z <= upperZ; z++) {
                            // null the z values
                            for (int x = 0; x < 16; x++, index++) {
                                arr[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                        }
                    }
                    if (trimX) {
                        index = indexY + lowerZi; // Skip blocks already removed by trimZ
                        for (int z = lowerZ; z <= upperZ; z++, index += 16) {
                            for (int x = lowerX; x <= upperX; x++) {
                                // null the x values
                                arr[index + x] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                        }
                    }
                }
                set.setBlocks(layer, arr);
            }
            BlockVector3 chunkPos = chunk.getChunkBlockCoord().withY(0);
            trimNBT(set, bv3 -> !this.contains(bv3), bv3 -> !this.contains(bv3.add(chunkPos)));
            return set;
        }
        return set;
    }
    //FAWE end

}
