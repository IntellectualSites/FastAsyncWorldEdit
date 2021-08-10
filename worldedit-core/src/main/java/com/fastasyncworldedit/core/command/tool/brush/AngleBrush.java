package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.function.mask.RadiusMask;
import com.fastasyncworldedit.core.function.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class AngleBrush implements Brush {

    private final int distance;

    public AngleBrush(int distance) {
        this.distance = distance;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        SurfaceMask surface = new SurfaceMask(editSession);
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);

        // replace \[angle]

    }

}
