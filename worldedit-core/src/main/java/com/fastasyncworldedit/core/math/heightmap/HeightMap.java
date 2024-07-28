package com.fastasyncworldedit.core.math.heightmap;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;

import java.util.concurrent.ThreadLocalRandom;

public interface HeightMap {

    double getHeight(int x, int z);

    void setSize(int size);

    default void perform(
            EditSession session,
            Mask mask,
            BlockVector3 pos,
            int size,
            int rotationMode,
            double yscale,
            boolean smooth,
            boolean towards,
            boolean layers
    ) throws MaxChangedBlocksException {
        int[][] data = generateHeightData(session, mask, pos, size, rotationMode, yscale, smooth, towards, layers);
        applyHeightMapData(data, session, pos, size, yscale, smooth, towards, layers);
    }

    default void applyHeightMapData(
            int[][] data,
            EditSession session,
            BlockVector3 pos,
            int size,
            double yscale,
            boolean smooth,
            boolean towards,
            boolean layers
    ) throws MaxChangedBlocksException {
        BlockVector3 top = session.getMaximumPoint();
        int maxY = top.y();
        Location min = new Location(session.getWorld(), pos.subtract(size, size, size).toVector3());
        BlockVector3 max = pos.add(size, maxY, size);
        Region region = new CuboidRegion(session.getWorld(), min.toBlockPoint(), max);
        com.sk89q.worldedit.math.convolution.HeightMap heightMap = new com.sk89q.worldedit.math.convolution.HeightMap(
                session,
                region,
                data[0],
                layers
        );
        if (smooth) {
            HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1));
            int diameter = 2 * size + 1;
            data[1] = filter.filter(data[1], diameter, diameter);
        }
        if (layers) {
            heightMap.applyLayers(data[1]);
        } else {
            heightMap.apply(data[1]);
        }
    }

    default int[][] generateHeightData(
            EditSession session,
            Mask mask,
            BlockVector3 pos,
            int size,
            final int rotationMode,
            double yscale,
            boolean smooth,
            boolean towards,
            final boolean layers
    ) {
        int maxY = session.getMaxY();
        int minY = session.getMinY();
        int diameter = 2 * size + 1;
        int centerX = pos.x();
        int centerZ = pos.z();
        int centerY = pos.y();
        int[] oldData = new int[diameter * diameter];
        int[] newData = new int[oldData.length];
        if (layers) { // Pixel accuracy
            centerY <<= 3;
            maxY <<= 3;
        }
        if (towards) {
            double sizePowInv = 1d / Math.pow(size, yscale);
            int targetY = pos.y();
            int tmpY = targetY;
            for (int x = -size; x <= size; x++) {
                int xx = centerX + x;
                for (int z = -size; z <= size; z++) {
                    int index = (z + size) * diameter + (x + size);
                    int zz = centerZ + z;
                    double raise = switch (rotationMode) {
                        default -> getHeight(x, z);
                        case 1 -> getHeight(z, x);
                        case 2 -> getHeight(-x, -z);
                        case 3 -> getHeight(-z, -x);
                    };
                    int height;
                    if (layers) {
                        height = tmpY = session.getNearestSurfaceLayer(xx, zz, tmpY, minY, maxY);
                    } else {
                        height = tmpY = session.getNearestSurfaceTerrainBlock(xx, zz, tmpY, minY, maxY);
                        if (height < minY) {
                            continue;
                        }
                    }
                    oldData[index] = height;
                    if (height == minY) {
                        newData[index] = centerY;
                        continue;
                    }
                    double raisePow = Math.pow(raise, yscale);
                    int diff = targetY - height;
                    double raiseScaled = diff * (raisePow * sizePowInv);
                    double raiseScaledAbs = Math.abs(raiseScaled);
                    int random =
                            (ThreadLocalRandom
                                    .current()
                                    .nextInt(maxY + 1 - minY) - minY) < (int) ((Math.ceil(raiseScaledAbs) - Math.floor(
                                    raiseScaledAbs)) * (maxY + 1 - minY)) ? (diff > 0 ? 1 : -1) : 0;
                    int raiseScaledInt = (int) raiseScaled + random;
                    newData[index] = height + raiseScaledInt;
                }
            }
        } else {
            int height = pos.y();
            for (int x = -size; x <= size; x++) {
                int xx = centerX + x;
                for (int z = -size; z <= size; z++) {
                    int index = (z + size) * diameter + (x + size);
                    int zz = centerZ + z;
                    double raise = switch (rotationMode) {
                        default -> getHeight(x, z);
                        case 1 -> getHeight(z, x);
                        case 2 -> getHeight(-x, -z);
                        case 3 -> getHeight(-z, -x);
                    };
                    if (layers) {
                        height = session.getNearestSurfaceLayer(xx, zz, height, minY, maxY);
                    } else {
                        height = session.getNearestSurfaceTerrainBlock(xx, zz, height, minY, maxY);
                        if (height < minY) {
                            continue;
                        }
                    }
                    oldData[index] = height;
                    if (height == minY) {
                        newData[index] = centerY;
                        continue;
                    }
                    raise = (yscale * raise);
                    int random =
                            (ThreadLocalRandom
                                    .current()
                                    .nextInt(maxY + 1 - minY) - minY) < (int) ((raise - (int) raise) * (maxY - minY + 1))
                                    ? 1 : 0;
                    int newHeight = height + (int) raise + random;
                    newData[index] = newHeight;
                }
            }
        }
        return new int[][]{oldData, newData};
    }

}
