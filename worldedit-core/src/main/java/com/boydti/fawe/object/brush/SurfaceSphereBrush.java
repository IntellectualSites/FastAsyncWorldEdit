package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.mask.RadiusMask;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import java.util.Arrays;

public class SurfaceSphereBrush implements Brush {
    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        SurfaceMask surface = new SurfaceMask(editSession);
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);
        RecursiveVisitor visitor = new RecursiveVisitor(vector -> surface.test(vector) && radius.test(vector), vector -> editSession.setBlock(vector, pattern));
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
    }
}