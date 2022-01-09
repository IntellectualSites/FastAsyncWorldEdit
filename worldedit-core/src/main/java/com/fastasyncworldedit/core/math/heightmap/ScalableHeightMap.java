package com.fastasyncworldedit.core.math.heightmap;

import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

public class ScalableHeightMap implements HeightMap {

    public int size2;
    public int size;
    protected int minY;
    protected int maxY;

    public enum Shape {
        CONE,
        CYLINDER,
    }

    /**
     * New height map.
     *
     * @param minY min y value allowed to be set. Inclusive.
     * @param maxY max y value allowed to be set. Inclusive.
     */
    public ScalableHeightMap(final int minY, final int maxY) {
        this.minY = minY;
        this.maxY = maxY;
        setSize(5);
    }

    @Override
    public void setSize(int size) {
        this.size = size;
        this.size2 = size * size;
    }

    @Override
    public double getHeight(int x, int z) {
        int dx = Math.abs(x);
        int dz = Math.abs(z);
        int d2 = dx * dx + dz * dz;
        if (d2 > size2) {
            return minY;
        }
        return Math.max(minY, size - MathMan.sqrtApprox(d2));
    }

    public static ScalableHeightMap fromShape(Shape shape, int minY, int maxY) {
        return switch (shape) {
            case CONE -> new ScalableHeightMap(minY, maxY);
            case CYLINDER -> new FlatScalableHeightMap(minY, maxY);
        };
    }

    public static ScalableHeightMap fromClipboard(Clipboard clipboard, int minY, int maxY) {
        BlockVector3 dim = clipboard.getDimensions();
        byte[][] heightArray = new byte[dim.getBlockX()][dim.getBlockZ()];
        int clipMinX = clipboard.getMinimumPoint().getBlockX();
        int clipMinZ = clipboard.getMinimumPoint().getBlockZ();
        int clipMinY = clipboard.getMinimumPoint().getBlockY();
        int clipMaxY = clipboard.getMaximumPoint().getBlockY();
        int clipHeight = clipMaxY - clipMinY + 1;
        HashSet<IntPair> visited = new HashSet<>();
        MutableBlockVector3 bv = new MutableBlockVector3();
        for (BlockVector3 pos : clipboard.getRegion()) {
            IntPair pair = new IntPair(pos.getBlockX(), pos.getBlockZ());
            if (visited.contains(pair)) {
                continue;
            }
            visited.add(pair);
            int xx = pos.getBlockX();
            int zz = pos.getBlockZ();
            int highestY = clipMinY;
            bv.setComponents(pos);
            for (int y = clipMinY; y <= clipMaxY; y++) {
                bv.mutY(y);
                BlockState block = clipboard.getBlock(bv);
                if (!block.getBlockType().getMaterial().isAir()) {
                    highestY = y + 1;
                }
            }
            int pointHeight = Math.min(clipMaxY, ((maxY - minY + 1) * (highestY - clipMinY)) / clipHeight);
            int x = xx - clipMinX;
            int z = zz - clipMinZ;
            heightArray[x][z] = (byte) pointHeight;
        }
        return new ArrayHeightMap(heightArray, minY, maxY);
    }

    public static ScalableHeightMap fromPNG(InputStream stream, int minY, int maxY) throws IOException {
        BufferedImage heightFile = MainUtil.readImage(stream);
        int width = heightFile.getWidth();
        int length = heightFile.getHeight();
        Raster data = heightFile.getData();
        byte[][] array = new byte[width][length];
        double third = 1 / 3.0;
        double alphaInverse = 1 / 255.0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int pixel = heightFile.getRGB(x, z);
                int red = pixel >> 16 & 0xFF;
                int green = pixel >> 8 & 0xFF;
                int blue = pixel >> 0 & 0xFF;
                int alpha = pixel >> 24 & 0xFF;
                int intensity = (int) (alpha * ((red + green + blue) * third) * alphaInverse);
                array[x][z] = (byte) intensity;
            }
        }
        return new ArrayHeightMap(array, minY, maxY);
    }

}
