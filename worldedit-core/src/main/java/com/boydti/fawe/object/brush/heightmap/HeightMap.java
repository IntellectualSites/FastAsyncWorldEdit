package com.boydti.fawe.object.brush.heightmap;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;

public interface HeightMap {
    public double getHeight(int x, int z);

    public void setSize(int size);


    default void perform(EditSession session, Mask mask, BlockVector3 pos, int size, int rotationMode, double yscale, boolean smooth, boolean towards, boolean layers) throws MaxChangedBlocksException {
        int[][] data = generateHeightData(session, mask, pos, size, rotationMode, yscale, smooth, towards, layers);
        applyHeightMapData(data, session, mask, pos, size, rotationMode, yscale, smooth, towards, layers);
    }

    default void applyHeightMapData(int[][] data, EditSession session, Mask mask, BlockVector3 pos, int size, int rotationMode, double yscale, boolean smooth, boolean towards, boolean layers) throws MaxChangedBlocksException {
    	BlockVector3 top = session.getMaximumPoint();
        int maxY = top.getBlockY();
        int diameter = 2 * size + 1;
        int iterations = 1;
        Location min = new Location(session.getWorld(), pos.subtract(size, maxY, size).toVector3());
        BlockVector3 max = pos.add(size, maxY, size);
        Region region = new CuboidRegion(session.getWorld(), min.toBlockPoint(), max);
        com.sk89q.worldedit.math.convolution.HeightMap heightMap = new com.sk89q.worldedit.math.convolution.HeightMap(session, region, data[0], layers);
        if (smooth) {
            try {
                HeightMapFilter filter = (HeightMapFilter) HeightMapFilter.class.getConstructors()[0].newInstance(GaussianKernel.class.getConstructors()[0].newInstance(5, 1));
                data[1] = filter.filter(data[1], diameter, diameter);
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        try {
            if (layers) {
                heightMap.applyLayers(data[1]);
            } else {
                heightMap.apply(data[1]);
            }
        } catch (MaxChangedBlocksException e) {
            throw e;
        }
    }

    default int[][] generateHeightData(EditSession session, Mask mask, BlockVector3 pos, int size, final int rotationMode, double yscale, boolean smooth, boolean towards, final boolean layers) {
    	BlockVector3 top = session.getMaximumPoint();
        int maxY = top.getBlockY();
        int diameter = 2 * size + 1;
        int centerX = pos.getBlockX();
        int centerZ = pos.getBlockZ();
        int centerY = pos.getBlockY();
        int endY = pos.getBlockY() + size;
        int startY = pos.getBlockY() - size;
        int[] oldData = new int[diameter * diameter];
        int[] newData = new int[oldData.length];
        if (layers) { // Pixel accuracy
            centerY <<= 3;
            maxY <<= 3;
        }
//        Vector mutablePos = new Vector(0, 0, 0);
        if (towards) {
            double sizePowInv = 1d / Math.pow(size, yscale);
            int targetY = pos.getBlockY();
            int tmpY = targetY;
            for (int x = -size; x <= size; x++) {
                int xx = centerX + x;
//                mutablePos.mutX(xx);
                for (int z = -size; z <= size; z++) {
                    int index = (z + size) * diameter + (x + size);
                    int zz = centerZ + z;
                    double raise;
                    switch (rotationMode) {
                        default:
                            raise = getHeight(x, z);
                            break;
                        case 1:
                            raise = getHeight(z, x);
                            break;
                        case 2:
                            raise = getHeight(-x, -z);
                            break;
                        case 3:
                            raise = getHeight(-z, -x);
                            break;
                    }
                    int height;
                    if (layers) {
                        height = tmpY = session.getNearestSurfaceLayer(xx, zz, tmpY, 0, maxY);
                    } else {
                        height = tmpY = session.getNearestSurfaceTerrainBlock(xx, zz, tmpY, 0, maxY);
                        if (height == -1) continue;
                    }
                    oldData[index] = height;
                    if (height == 0) {
                        newData[index] = centerY;
                        continue;
                    }
                    double raisePow = Math.pow(raise, yscale);
                    int diff = targetY - height;
                    double raiseScaled = diff * (raisePow * sizePowInv);
                    double raiseScaledAbs = Math.abs(raiseScaled);
                    int random = PseudoRandom.random.random(256) < (int) ((Math.ceil(raiseScaledAbs) - Math.floor(raiseScaledAbs)) * 256) ? (diff > 0 ? 1 : -1) : 0;
                    int raiseScaledInt = (int) raiseScaled + random;
                    newData[index] = height + raiseScaledInt;
                }
            }
        } else {
            int height = pos.getBlockY();
            for (int x = -size; x <= size; x++) {
                int xx = centerX + x;
//                mutablePos.mutX(xx);
                for (int z = -size; z <= size; z++) {
                    int index = (z + size) * diameter + (x + size);
                    int zz = centerZ + z;
                    double raise;
                    switch (rotationMode) {
                        default:
                            raise = getHeight(x, z);
                            break;
                        case 1:
                            raise = getHeight(z, x);
                            break;
                        case 2:
                            raise = getHeight(-x, -z);
                            break;
                        case 3:
                            raise = getHeight(-z, -x);
                            break;
                    }
                    if (layers) {
                        height = session.getNearestSurfaceLayer(xx, zz, height, 0, maxY);
                    } else {
                        height = session.getNearestSurfaceTerrainBlock(xx, zz, height, 0, maxY);
                        if (height == -1) continue;
                    }
                    oldData[index] = height;
                    if (height == 0) {
                        newData[index] = centerY;
                        continue;
                    }
                    raise = (yscale * raise);
                    int random = PseudoRandom.random.random(256) < (int) ((raise - (int) raise) * (256)) ? 1 : 0;
                    int newHeight = height + (int) raise + random;
                    newData[index] = newHeight;
                }
            }
        }
        return new int[][]{oldData, newData};
    }
}
