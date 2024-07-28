package com.fastasyncworldedit.core.math.heightmap;

import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

public class ScalableHeightMap implements HeightMap {

    public int size2;
    public int size;

    public enum Shape {
        CONE,
        CYLINDER,
    }

    /**
     * New height map. "Normalised" to a min Y of zero.
     */
    public ScalableHeightMap() {
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
            return 0;
        }
        return Math.max(0, size - MathMan.sqrtApprox(d2));
    }

    public static ScalableHeightMap fromShape(Shape shape) {
        return switch (shape) {
            case CONE -> new ScalableHeightMap();
            case CYLINDER -> new FlatScalableHeightMap();
        };
    }

    public static ScalableHeightMap fromClipboard(Clipboard clipboard, int minY, int maxY) {
        BlockVector3 dim = clipboard.getDimensions();
        char[][] heightArray = new char[dim.x()][dim.z()];
        int clipMinX = clipboard.getMinimumPoint().x();
        int clipMinZ = clipboard.getMinimumPoint().z();
        int clipMinY = clipboard.getMinimumPoint().y();
        int clipMaxY = clipboard.getMaximumPoint().y();
        int clipHeight = clipMaxY - clipMinY + 1;
        HashSet<IntPair> visited = new HashSet<>();
        MutableBlockVector3 bv = new MutableBlockVector3();
        for (BlockVector3 pos : clipboard.getRegion()) {
            IntPair pair = new IntPair(pos.x(), pos.z());
            if (visited.contains(pair)) {
                continue;
            }
            visited.add(pair);
            int xx = pos.x();
            int zz = pos.z();
            int highestY = clipMinY;
            bv.setComponents(pos);
            for (int y = clipMinY; y <= clipMaxY; y++) {
                bv.mutY(y);
                BlockState block = clipboard.getBlock(bv);
                if (!block.getBlockType().getMaterial().isAir()) {
                    highestY = y + 1;
                }
            }
            int x = xx - clipMinX;
            int z = zz - clipMinZ;
            heightArray[x][z] = (char) Math.min(clipMaxY, ((maxY - minY + 1) * (highestY - clipMinY)) / clipHeight);
        }
        return new ArrayHeightMap(heightArray, maxY - minY + 1);
    }

    public static ScalableHeightMap fromPNG(InputStream stream) throws IOException {
        BufferedImage heightFile = MainUtil.readImage(stream);
        int width = heightFile.getWidth();
        int length = heightFile.getHeight();
        char[][] array = new char[width][length];
        double third = 1 / 3.0;
        double alphaInverse = 1 / 255.0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int pixel = heightFile.getRGB(x, z);
                int red = pixel >> 16 & 0xFF;
                int green = pixel >> 8 & 0xFF;
                int blue = pixel & 0xFF;
                int alpha = pixel >> 24 & 0xFF;
                array[x][z] = (char) (alpha * ((red + green + blue) * third) * alphaInverse);
            }
        }
        return new ArrayHeightMap(array, 256d);
    }

}
