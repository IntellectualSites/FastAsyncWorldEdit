package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.math.heightmap.HeightMap;
import com.fastasyncworldedit.core.math.heightmap.RotatableHeightMap;
import com.fastasyncworldedit.core.math.heightmap.ScalableHeightMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

public class HeightBrush implements Brush {

    private HeightMap heightMap;
    private boolean randomRotate;
    public final int rotation;
    public final double yscale;
    public final boolean layers;
    public final boolean smooth;

    public HeightBrush(
            InputStream stream, int rotation, double yscale, boolean layers, boolean smooth, Clipboard clipboard,
            int minY, int maxY
    ) {
        this(stream, rotation, yscale, layers, smooth, clipboard, ScalableHeightMap.Shape.CONE, minY, maxY);
    }

    public HeightBrush(
            InputStream stream,
            int rotation,
            double yscale,
            boolean layers,
            boolean smooth,
            Clipboard clipboard,
            ScalableHeightMap.Shape shape,
            int minY,
            int maxY
    ) {
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        this.layers = layers;
        this.smooth = smooth;
        if (stream != null) {
            try {
                heightMap = ScalableHeightMap.fromPNG(stream);
            } catch (IOException e) {
                throw new FaweException(Caption.of("fawe.worldedit.brush.brush.height.invalid", e.getMessage()));
            }
        } else if (clipboard != null) {
            heightMap = ScalableHeightMap.fromClipboard(clipboard, minY, maxY);
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
            rotatable.rotate(ThreadLocalRandom.current().nextInt(360));
        }
        return heightMap;
    }

    public void setRandomRotate(boolean randomRotate) {
        this.randomRotate = randomRotate;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double sizeDouble) throws
            MaxChangedBlocksException {
        int size = (int) sizeDouble;
        HeightMap map = getHeightMap();
        map.setSize(size);

        Mask mask = editSession.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        map.perform(editSession, mask, position, size, rotation, yscale, smooth, false, layers);
    }

}
