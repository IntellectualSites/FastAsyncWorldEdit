package com.fastasyncworldedit.core.regions;

import com.fastasyncworldedit.core.anvil.MCAFile;
import com.fastasyncworldedit.core.anvil.MCAWorld;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.task.RunnableVal4;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class WorldRegionsRegion implements Region {

    private final MCAWorld world;
    private final BlockVector3 min;
    private final BlockVector3 max;
    private Set<BlockVector2> chunks = null;

    public WorldRegionsRegion(@Nonnull final MCAWorld world) {
        this.world = Objects.requireNonNull(world);
        List<Path> regions = world.getRegionFileFiles();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Path p : regions) {
            String[] split = p.getFileName().toString().split("\\.");
            int x = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            minX = Math.min(x, minX);
            minZ = Math.min(z, minZ);
            maxX = Math.max(x, maxX);
            maxZ = Math.max(z, maxZ);
        }
        this.min = BlockVector3.at(
                minX,
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMinY(),
                minZ
        );
        this.max = BlockVector3.at(
                maxX,
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMaxY(),
                maxZ
        );
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        Queue<BlockVector2> queue = new ArrayDeque<>(getChunks());
        return new Iterator<>() {
            private final int by = min.y();
            private final int ty = max.y();
            private final MutableBlockVector3 mutable = new MutableBlockVector3();

            private BlockVector2 chunk = queue.poll();
            private int cx = chunk.getX() << 4;
            private int cz = chunk.getZ() << 4;
            private int x;
            private int y;
            private int z;

            @Override
            public boolean hasNext() {
                return x < 15 || y < ty || z < 15 || queue.peek() != null;
            }

            @Override
            public MutableBlockVector3 next() {
                int curX = x;
                int curY = y;
                int curZ = z;
                if (++x > 15) {
                    if (++z > 15) {
                        if (++y > ty) {
                            if (!hasNext()) {
                                throw new NoSuchElementException("End of iterator") {
                                    @Override
                                    public Throwable fillInStackTrace() {
                                        return this;
                                    }
                                };
                            }
                            chunk = queue.poll();
                            x = 0;
                            y = by;
                            z = 0;
                            cx = chunk.getX() << 4;
                            cz = chunk.getZ() << 4;
                            return mutable.setComponents(cx + x, y, cz + z);
                        } else {
                            x = 0;
                            z = 0;
                        }
                    } else {
                        x = 0;
                    }
                }
                return mutable.setComponents(cx + curX, curY, cz + curZ);
            }
        };
    }

    @Override
    public void setWorld(final World world) {
        throw new UnsupportedOperationException("Cannot modify WorldRegionsRegion - Immutable");
    }

    @Override
    public Region clone() {
        return new WorldRegionsRegion(world);
    }

    @Override
    public List<BlockVector2> polygonize(final int maxPoints) {
        return null;
    }

    @Override
    public void shift(final BlockVector3 change) throws RegionOperationException {
        throw new UnsupportedOperationException("Cannot modify WorldRegionsRegion - Immutable");
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return min;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return max;
    }

    /**
     * Get X-size.
     *
     * @return width
     */
    @Override
    public int getWidth() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.x() - min.x() + 1;
    }

    /**
     * Get Y-size.
     *
     * @return height
     */
    @Override
    public int getHeight() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.y() - min.y() + 1;
    }

    /**
     * Get Z-size.
     *
     * @return length
     */
    @Override
    public int getLength() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.z() - min.z() + 1;
    }

    @Override
    public void expand(final BlockVector3... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Cannot modify WorldRegionsRegion - Immutable");
    }

    @Override
    public void contract(final BlockVector3... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Cannot modify WorldRegionsRegion - Immutable");
    }

    @Override
    public boolean contains(final BlockVector3 position) {
        return false;
    }

    @Override
    public Set<BlockVector2> getChunks() {
        if (chunks == null) {
            synchronized (this) {
                if (chunks != null) {
                    return chunks;
                }
                Set<BlockVector2> tmp = new HashSet<>();
                for (MCAFile mca : world.getMCAs()) {
                    mca.forEachChunk(new RunnableVal4<>() {
                        @Override
                        public void run(Integer x, Integer z, Integer offset, Integer size) {
                            if (offset != 0 && size > 0) {
                                tmp.add(BlockVector2.at(x, z));
                            }
                        }
                    });
                }
                chunks = tmp;
            }
        }
        return chunks;
    }

    @Override
    public Set<BlockVector3> getChunkCubes() {
        return new AbstractSet<>() {
            @Override
            public Iterator<BlockVector3> iterator() {
                Queue<BlockVector2> chunks = new ArrayDeque<>(getChunks());

                return new Iterator<>() {
                    private final MutableBlockVector3 mutable = new MutableBlockVector3();
                    private final int by = min.y() >> 4;
                    private final int ty = max.y() >> 4;

                    private BlockVector2 chunk = chunks.poll();
                    private int y;

                    @Override
                    public boolean hasNext() {
                        return y < ty || chunks.peek() != null;
                    }

                    @Override
                    public BlockVector3 next() {
                        int curY = y;
                        if (++y > ty) {
                            if (!hasNext()) {
                                throw new NoSuchElementException("End of iterator") {
                                    @Override
                                    public Throwable fillInStackTrace() {
                                        return this;
                                    }
                                };
                            }
                            y = by;
                            chunk = chunks.poll();
                            return mutable.setComponents(chunk.getX(), y, chunk.getZ());
                        }
                        return mutable.setComponents(chunk.getX(), curY, chunk.getZ());
                    }
                };
            }

            @Override
            public int size() {
                return getChunks().size() * ((max.y() >> 4) - (min.y() >> 4));
            }
        };
    }

    @Nullable
    @Override
    public World getWorld() {
        return null;
    }

}
