package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.boydti.fawe.object.pattern.BiomePattern;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

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
            Pattern tmp;
            try {
                tmp = p.apply(position);
            } catch (BiomePattern.BiomePatternException ignore) {
                tmp = ignore.getPattern();
            }
            finalPattern = tmp;
        } else {
            finalPattern = p;
        }
        final int size2 = (int) (size * size);
        SurfaceMask surface = new SurfaceMask(editSession);
        final SolidBlockMask solid = new SolidBlockMask(editSession);

        RecursiveVisitor visitor = new RecursiveVisitor(vector -> {
            double dist = vector.distanceSq(position);
            if (dist < size2 && !placed.contains(vector) && (ThreadLocalRandom.current().nextInt(5) < 2) && surface.test(vector)) {
                placed.add(vector);
                return true;
            }
            return false;
        }, vector -> editSession.setBlock(vector, finalPattern), recursion);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        visitor.visit(position);
        Operations.completeBlindly(visitor);
    }
}
