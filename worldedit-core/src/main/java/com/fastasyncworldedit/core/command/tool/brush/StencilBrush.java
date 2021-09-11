package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.math.heightmap.HeightMap;
import com.fastasyncworldedit.core.function.mask.StencilBrushMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Location;

import java.io.InputStream;
import java.util.Arrays;

public class StencilBrush extends HeightBrush {

    private final boolean onlyWhite;

    public StencilBrush(
            InputStream stream, int rotation, double yscale, boolean onlyWhite, Clipboard clipboard, int minY,
            int maxY
    ) {
        super(stream, rotation, yscale, false, true, clipboard, minY, maxY);
        this.onlyWhite = onlyWhite;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 center, Pattern pattern, double sizeDouble) throws
            MaxChangedBlocksException {
        int size = (int) sizeDouble;
        int size2 = (int) (sizeDouble * sizeDouble);
        int maxY = editSession.getMaxY();
        int minY = editSession.getMinY();
        int add;
        if (yscale < 0) {
            add = maxY - minY;
        } else {
            add = 0;
        }
        final HeightMap map = getHeightMap();
        map.setSize(size);
        int cutoff = onlyWhite ? maxY - minY : 0;
        final SolidBlockMask solid = new SolidBlockMask(editSession);

        Location loc = editSession.getPlayer().getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        AffineTransform transform = new AffineTransform().rotateY((-yaw) % 360).rotateX(pitch - 90).inverse();

        double scale = (yscale / sizeDouble) * (maxY - minY + 1);
        RecursiveVisitor visitor =
                new RecursiveVisitor(new StencilBrushMask(
                        editSession,
                        solid,
                        center,
                        transform,
                        size2,
                        map,
                        scale,
                        add,
                        cutoff,
                        maxY,
                        pattern
                ),
                        vector -> true, Integer.MAX_VALUE, editSession.getMinY(), editSession.getMaxY()
                );
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        visitor.visit(center);
        Operations.completeBlindly(visitor);
    }

}
