package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.mask.RadiusMask;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.Pattern;

public class AngleBrush implements Brush {

    private final int distance;

    public AngleBrush(int distance) {
        this.distance = distance;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        SurfaceMask surface = new SurfaceMask(editSession);
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);

        // replace \[angle]

    }
}
