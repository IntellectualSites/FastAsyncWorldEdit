package com.sk89q.worldedit.math.convolution;

import com.boydti.fawe.object.visitor.Fast2DIterator;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.registry.state.PropertyGroup;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Iterator;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows applications of Kernels onto the region's height map.
 * <p>
 * <p>Currently only used for smoothing (with a GaussianKernel)</p>.
 */
public class HeightMap {

    private final boolean layers;
    private int[] data;
    private boolean[] invalid;
    private int width;
    private int height;

    private Region region;
    private EditSession session;

    /**
     * Constructs the HeightMap
     *
     * @param session an edit session
     * @param region  the region
     */
    public HeightMap(EditSession session, Region region) {
        this(session, region, false);
    }

    public HeightMap(EditSession session, Region region, boolean naturalOnly) {
        this(session, region, naturalOnly, false);
    }

    public HeightMap(EditSession session, Region region, boolean naturalOnly, boolean layers) {
        checkNotNull(session);
        checkNotNull(region);

        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        this.layers = layers;

        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxY = region.getMaximumPoint().getBlockY();

        data = new int[width * height];
        invalid = new boolean[data.length];

        if (layers) {
        	BlockVector3 min = region.getMinimumPoint();
        	BlockVector3 max = region.getMaximumPoint();
            int bx = min.getBlockX();
            int bz = min.getBlockZ();
            Iterable<BlockVector2> flat = Regions.asFlatRegion(region).asFlatRegion();
            Iterator<BlockVector2> iter = new Fast2DIterator(flat, session).iterator();
            int layer = 0;
            while (iter.hasNext()) {
                BlockVector2 pos = iter.next();
                int x = pos.getBlockX();
                int z = pos.getBlockZ();
                layer = session.getNearestSurfaceLayer(x, z, (layer + 7) >> 3, 0, maxY);
                data[(z - bz) * width + (x - bx)] = layer;
            }
        } else {
            // Store current heightmap data
            int index = 0;
            if (naturalOnly) {
                for (int z = 0; z < height; ++z) {
                    for (int x = 0; x < width; ++x, index++) {
                        data[index] = session.getHighestTerrainBlock(x + minX, z + minZ, minY, maxY);
                    }
                }
            } else {
                int yTmp = 255;
                for (int z = 0; z < height; ++z) {
                    for (int x = 0; x < width; ++x, index++) {
                        yTmp = session.getNearestSurfaceTerrainBlock(x + minX, z + minZ, yTmp, minY, maxY, Integer.MIN_VALUE, Integer.MAX_VALUE);
                        switch (yTmp) {
                            case Integer.MIN_VALUE:
                                yTmp = minY;
                                invalid[index] = true;
                                break;
                            case Integer.MAX_VALUE:
                                yTmp = maxY;
                                invalid[index] = true;
                                break;
                        }
                        data[index] = yTmp;
                    }
                }
            }
        }
    }

    @Deprecated
    public HeightMap(EditSession session, Region region, int[] data, boolean layers) {
        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        this.data = data;

        this.layers = layers;
    }

    /**
     * Apply the filter 'iterations' amount times.
     *
     * @param filter     the filter
     * @param iterations the number of iterations
     * @return number of blocks affected
     * @throws MaxChangedBlocksException
     */
    public int applyFilter(HeightMapFilter filter, int iterations) throws MaxChangedBlocksException {
        checkNotNull(filter);

        int[] newData = new int[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);

        for (int i = 0; i < iterations; ++i) {
            newData = filter.filter(newData, width, height);
        }

        return layers ? applyLayers(newData) : apply(newData);
    }

    public int applyLayers(int[] data) {
        checkNotNull(data);

        BlockVector3 minY = region.getMinimumPoint();
        int originX = minY.getBlockX();
        int originY = minY.getBlockY();
        int originZ = minY.getBlockZ();

        int maxY = region.getMaximumPoint().getBlockY();
        BlockStateHolder fillerAir = EditSession.nullBlock;

        int blocksChanged = 0;

        BlockStateHolder tmpBlock = EditSession.nullBlock;

        // Apply heightmap
        int maxY4 = maxY << 4;
        int index = 0;

        for (int z = 0; z < height; ++z) {
            int zr = z + originZ;
            for (int x = 0; x < width; ++x) {
                int curHeight = this.data[index];
                if (this.invalid != null && this.invalid[index]) continue;
                int newHeight = Math.min(maxY4, data[index++]);
                int curBlock = (curHeight) >> 4;
                int newBlock = (newHeight + 15) >> 4;
                int xr = x + originX;

                // Depending on growing or shrinking we need to start at the bottom or top
                if (newHeight > curHeight) {
                    // Set the top block of the column to be the same type (this might go wrong with rounding)
//<<<<<<< HEAD
                    BlockStateHolder existing = session.getBlock(xr, curBlock, zr);

                    // Skip water/lava
                    if (existing.getBlockType().getMaterial().isMovementBlocker()) {
                        // Grow -- start from 1 below top replacing airblocks
                        for (int setY = newBlock - 1, getY = curBlock; setY >= curBlock; --setY, getY--) {
                            BlockStateHolder get = session.getBlock(xr, getY, zr);
                            if (get != EditSession.nullBlock) tmpBlock = get;
                            session.setBlock(xr, setY, zr, tmpBlock);
                            ++blocksChanged;
                        }
                        int setData = newHeight & 15;
                        if (setData != 0) {
                            existing = PropertyGroup.LEVEL.set(existing, setData - 1);
                            session.setBlock(xr, newBlock, zr, existing);
                            ++blocksChanged;
                        } else {
                            existing = PropertyGroup.LEVEL.set(existing, 15);
                            session.setBlock(xr, newBlock, zr, existing);

//=======
//                    BlockState existing = session.getBlock(BlockVector3.at(xr, curHeight, zr));
//
//                    // Skip water/lava
//                    if (existing.getBlockType() != BlockTypes.WATER && existing.getBlockType() != BlockTypes.LAVA) {
//                        session.setBlock(BlockVector3.at(xr, newHeight, zr), existing);
//                        ++blocksChanged;
//
//                        // Grow -- start from 1 below top replacing airblocks
//                        for (int y = newHeight - 1 - originY; y >= 0; --y) {
//                            int copyFrom = (int) (y * scale);
//                            session.setBlock(BlockVector3.at(xr, originY + y, zr), session.getBlock(BlockVector3.at(xr, originY + copyFrom, zr)));
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
                            ++blocksChanged;
                        }
                    }
                } else if (curHeight > newHeight) {
//<<<<<<< HEAD
                    // Fill rest with air
                    for (int y = newBlock + 1; y <= ((curHeight + 15) >> 4); ++y) {
                        session.setBlock(xr, y, zr, fillerAir);
//=======
//                    // Shrink -- start from bottom
//                    for (int y = 0; y < newHeight - originY; ++y) {
//                        int copyFrom = (int) (y * scale);
//                        session.setBlock(BlockVector3.at(xr, originY + y, zr), session.getBlock(BlockVector3.at(xr, originY + copyFrom, zr)));
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
                        ++blocksChanged;
                    }
                    // Set the top block of the column to be the same type
                    // (this could otherwise go wrong with rounding)
                    int setData = newHeight & 15;
                    BlockStateHolder existing = session.getBlock(xr, curBlock, zr);
                    if (setData != 0) {
                        existing = PropertyGroup.LEVEL.set(existing, setData - 1);
                        session.setBlock(xr, newBlock, zr, existing);
                    } else {
                        existing = PropertyGroup.LEVEL.set(existing, 15);
                        session.setBlock(xr, newBlock, zr, existing);
                    }
                    ++blocksChanged;
                }
            }
        }
        return blocksChanged;
    }

    public int apply(int[] data) throws MaxChangedBlocksException {
        checkNotNull(data);

        BlockVector3 minY = region.getMinimumPoint();
        int originX = minY.getBlockX();
        int originY = minY.getBlockY();
        int originZ = minY.getBlockZ();

        int maxY = region.getMaximumPoint().getBlockY();
        BlockStateHolder fillerAir = EditSession.nullBlock;

        int blocksChanged = 0;

        BlockStateHolder tmpBlock = EditSession.nullBlock;

        // Apply heightmap
        int index = 0;
        for (int z = 0; z < height; ++z) {
            int zr = z + originZ;
            for (int x = 0; x < width; ++x, index++) {
                int curHeight = this.data[index];
                if (this.invalid != null && this.invalid[index]) continue;
                int newHeight = Math.min(maxY, data[index]);

                int xr = x + originX;

                // Depending on growing or shrinking we need to start at the bottom or top
                if (newHeight > curHeight) {
                    // Set the top block of the column to be the same type (this might go wrong with rounding)
                    BlockStateHolder existing = session.getBlock(xr, curHeight, zr);


                    // Skip water/lava
                    if (existing.getBlockType().getMaterial().isMovementBlocker()) {
                        int y0 = newHeight - 1;
                        for (int setY = y0, getY = curHeight - 1; setY >= curHeight; setY--, getY--) {
                            BlockStateHolder get = session.getBlock(xr, getY, zr);
                            if (get != EditSession.nullBlock) tmpBlock = get;
                            session.setBlock(xr, setY, zr, tmpBlock);
                            ++blocksChanged;
                        }
                        session.setBlock(xr, newHeight, zr, existing);
                        ++blocksChanged;
                    }
                } else if (curHeight > newHeight) {
                    // Set the top block of the column to be the same type
                    // (this could otherwise go wrong with rounding)
                    session.setBlock(BlockVector3.at(xr, newHeight, zr), session.getBlock(BlockVector3.at(xr, curHeight, zr)));
                    ++blocksChanged;

                    // Fill rest with air
                    for (int y = newHeight + 1; y <= curHeight; ++y) {
                        session.setBlock(BlockVector3.at(xr, y, zr), fillerAir);
                        ++blocksChanged;
                    }
                }
            }
        }
        return blocksChanged;
    }



}
