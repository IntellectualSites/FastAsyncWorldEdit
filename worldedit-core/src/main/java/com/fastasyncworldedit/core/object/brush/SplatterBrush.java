package com.fastasyncworldedit.core.object.brush;

import com.fastasyncworldedit.core.object.brush.mask.SplatterBrushMask;
import com.fastasyncworldedit.core.object.collection.LocalBlockVectorSet;
import com.fastasyncworldedit.core.object.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;

public class SplatterBrush extends ScatterBrush {
    private final boolean solid;
    private final int recursion;

    public SplatterBrush(int count, int distance, boolean solid) {
        super(count, 1);
        this.recursion = distance;
        this.solid = solid;
    }

    @Override
    public void apply(final EditSession editSession, final LocalBlockVectorSet placed, final BlockVector3 position, Pattern p, double size) throws MaxChangedBlocksException {
        final Pattern finalPattern;
        if (solid) {
            finalPattern = p.apply(position);
        } else {
            finalPattern = p;
        }
        final int size2 = (int) (size * size);
        SurfaceMask surface = new SurfaceMask(editSession);

        RecursiveVisitor visitor = new RecursiveVisitor(new SplatterBrushMask(editSession, position, size2, surface, placed),
            vector -> editSession.setBlock(vector, finalPattern), recursion);
        visitor.setMaxBranch(2);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        visitor.visit(position);
        Operations.completeBlindly(visitor);
    }
}
