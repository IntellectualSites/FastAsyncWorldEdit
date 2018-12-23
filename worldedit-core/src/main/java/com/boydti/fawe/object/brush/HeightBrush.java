package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.brush.heightmap.RotatableHeightMap;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

import java.io.IOException;
import java.io.InputStream;

public class HeightBrush implements Brush {

    private HeightMap heightMap;
    private boolean randomRotate;
    public final int rotation;
    public final double yscale;
    public final boolean layers;
    public final boolean smooth;

    public HeightBrush(InputStream stream, int rotation, double yscale, boolean layers, boolean smooth, Clipboard clipboard) {
        this(stream, rotation, yscale, layers, smooth, clipboard, ScalableHeightMap.Shape.CONE);
    }

    public HeightBrush(InputStream stream, int rotation, double yscale, boolean layers, boolean smooth, Clipboard clipboard, ScalableHeightMap.Shape shape) {
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        this.layers = layers;
        this.smooth = smooth;
        if (stream != null) {
            try {
                heightMap = ScalableHeightMap.fromPNG(stream);
            } catch (IOException e) {
                throw new FaweException(BBC.BRUSH_HEIGHT_INVALID);
            }
        } else if (clipboard != null) {
            heightMap = ScalableHeightMap.fromClipboard(clipboard);
        } else {
            heightMap = ScalableHeightMap.fromShape(shape);
        }
    }

    public HeightMap getHeightMap() {
        if (randomRotate) {
            if (!(heightMap instanceof RotatableHeightMap)) {
                heightMap = new RotatableHeightMap(heightMap);
            }
            RotatableHeightMap rotatable = (RotatableHeightMap) heightMap;
            rotatable.rotate(PseudoRandom.random.nextInt(360));
        }
        return heightMap;
    }

    public void setRandomRotate(boolean randomRotate) {
        this.randomRotate = randomRotate;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) sizeDouble;
        HeightMap map = getHeightMap();
        map.setSize(size);

        FaweQueue queue = editSession.getQueue();
        // Optimized application of height map
        if (queue instanceof HeightMapMCAGenerator) {
            HeightMapMCAGenerator hmmg = (HeightMapMCAGenerator) queue;

            byte[] metaHeight = hmmg.getMetaData().getMeta("PRECISION_HEIGHT");
            if (metaHeight == null) {
                hmmg.getMetaData().setMeta("PRECISION_HEIGHT", metaHeight = new byte[hmmg.getArea()]);
            }

            Vector3 origin = hmmg.getOrigin();

            int bx = position.getBlockX();
            int bz = position.getBlockZ();

            int minIndex = -(size * 2) - 1;
            int width = hmmg.getWidth();

            int minX = Math.max(-size, -bx);
            int minZ = Math.max(-size, -bz);
            int maxX = Math.min(size, hmmg.getWidth() - 1 - bx);
            int maxZ = Math.min(size, hmmg.getLength() - 1 - bz);

            int zIndex = (bz + minZ) * width;
            for (int z = minZ; z <= maxZ; z++, zIndex += width) {
                int zz = bz + z;
                int index = zIndex + (bx + minX);
                if (index < minIndex) continue;
                if (index >= metaHeight.length) break;
                for (int x = minX; x <= maxX; x++, index++) {
                    if (index < 0) continue;
                    if (index >= metaHeight.length) break;

                    int xx = bx + x;
                    int currentBlockHeight = hmmg.getHeight(index);
                    int currentLayer = metaHeight[index] & 0xFF;

                    double addHeight = heightMap.getHeight(x, z) * yscale;
                    int addBlockHeight = (int) addHeight;
                    int addLayer = (int) ((addHeight - addBlockHeight) * 256);

                    int newLayer = addLayer + currentLayer;
                    int newBlockHeight = currentBlockHeight + addBlockHeight;

                    int newLayerAbs = MathMan.absByte(newLayer);

                    if (newLayerAbs >= 256) {
                        int newLayerBlocks = (newLayer >> 8);
                        newBlockHeight += newLayerBlocks;
                        newLayer -= newLayerBlocks << 8;
                    }

                    hmmg.setHeight(index, newBlockHeight);
                    metaHeight[index] = (byte) newLayer;
                }
            }

            if (smooth) {
            	BlockVector2 min = new BlockVector2(Math.max(0, bx - size), Math.max(0, bz - size));
            	BlockVector2 max = new BlockVector2(Math.min(hmmg.getWidth() - 1, bx + size), Math.min(hmmg.getLength() - 1, bz + size));
                hmmg.smooth(min, max, 8, 1);

                if (size > 20) {
                    int smoothSize = size + 8;
                    min = new BlockVector2(Math.max(0, bx - smoothSize), Math.max(0, bz - smoothSize));
                    max = new BlockVector2(Math.min(hmmg.getWidth() - 1, bx + smoothSize), Math.min(hmmg.getLength() - 1, bz + smoothSize));
                    hmmg.smooth(min, max, 1, 1);
                }
            }

            return;
        }
        Mask mask = editSession.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        map.perform(editSession, mask, position, size, rotation, yscale, smooth, false, layers);
    }
}
