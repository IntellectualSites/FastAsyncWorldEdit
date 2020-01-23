package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

import java.util.concurrent.ThreadLocalRandom;

public class ShatterBrush extends ScatterBrush {
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    public ShatterBrush(int count) {
        super(count, 1);
    }

    @Override
    public void apply(final EditSession editSession, final LocalBlockVectorSet placed, final BlockVector3 position, Pattern p, double size) throws MaxChangedBlocksException {
    }

    @Override
    public void finish(EditSession editSession, LocalBlockVectorSet placed, final BlockVector3 position, Pattern pattern, double size) {
        int radius2 = (int) (size * size);
        // Individual frontier for each point
        LocalBlockVectorSet[] frontiers = new LocalBlockVectorSet[placed.size()];
        // Keep track of where each frontier has visited
        LocalBlockVectorSet[] frontiersVisited = new LocalBlockVectorSet[placed.size()];
        // Initiate the frontier with the starting points
        int i = 0;
        for (BlockVector3 pos : placed) {
            LocalBlockVectorSet set = new LocalBlockVectorSet();
            set.add(pos);
            frontiers[i] = set;
            frontiersVisited[i] = set.clone();
            i++;
        }
        // Mask
        Mask mask = editSession.getMask();
        if (mask == null) {
            mask = Masks.alwaysTrue();
        }
        final Mask finalMask = mask;
        final SurfaceMask surfaceTest = new SurfaceMask(editSession);
        // Expand
        boolean notEmpty = true;
        // Keep track of where we've visited
        LocalBlockVectorSet tmp = new LocalBlockVectorSet();
        while (notEmpty) {
            notEmpty = false;
            for (i = 0; i < frontiers.length; i++) {
                LocalBlockVectorSet frontier = frontiers[i];
                notEmpty |= !frontier.isEmpty();
                final LocalBlockVectorSet frontierVisited = frontiersVisited[i];
                // This is a temporary set with the next blocks the frontier will visit
                final LocalBlockVectorSet finalTmp = tmp;
                frontier.forEach((x, y, z, index) -> {
                    if (ThreadLocalRandom.current().nextInt(2) == 0) {
                        finalTmp.add(x, y, z);
                        return;
                    }
                    for (int i1 = 0; i1 < BreadthFirstSearch.DIAGONAL_DIRECTIONS.length; i1++) {
                        BlockVector3 direction = BreadthFirstSearch.DIAGONAL_DIRECTIONS[i1];
                        int x2 = x + direction.getBlockX();
                        int y2 = y + direction.getBlockY();
                        int z2 = z + direction.getBlockZ();
                        // Check boundary
                        int dx = position.getBlockX() - x2;
                        int dy = position.getBlockY() - y2;
                        int dz = position.getBlockZ() - z2;
                        int dSqr = (dx * dx) + (dy * dy) + (dz * dz);
                        if (dSqr <= radius2) {
                            BlockVector3 bv = mutable.setComponents(x2, y2, z2);
                            if (surfaceTest.test(editSession, bv) && finalMask.test(editSession, bv)) {
                                // (collision) If it's visited and part of another frontier, set the block
                                if (!placed.add(x2, y2, z2)) {
                                    if (!frontierVisited.contains(x2, y2, z2)) {
                                        editSession.setBlock(x2, y2, z2, pattern);
                                    }
                                } else {
                                    // Hasn't visited and not a collision = add it
                                    finalTmp.add(x2, y2, z2);
                                    frontierVisited.add(x2, y2, z2);
                                }
                            }
                        }
                    }
                });
                // Swap the frontier with the temporary set
                frontiers[i] = tmp;
                tmp = frontier;
                tmp.clear();
            }
        }
    }
}
