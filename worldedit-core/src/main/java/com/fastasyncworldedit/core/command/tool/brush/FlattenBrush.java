package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.math.heightmap.HeightMap;
import com.fastasyncworldedit.core.math.heightmap.ScalableHeightMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.io.InputStream;

public class FlattenBrush extends HeightBrush {

    public FlattenBrush(
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
        super(stream, rotation, yscale, layers, smooth, clipboard, shape, minY, maxY);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double sizeDouble) throws
            MaxChangedBlocksException {
        int size = (int) sizeDouble;
        Mask mask = editSession.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        HeightMap map = getHeightMap();
        map.setSize(size);
        map.perform(editSession, mask, position, size, rotation, yscale, smooth, true, layers);
    }

}
