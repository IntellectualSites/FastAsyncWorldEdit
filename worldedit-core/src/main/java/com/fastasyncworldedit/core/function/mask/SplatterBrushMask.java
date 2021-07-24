package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.concurrent.ThreadLocalRandom;

public class SplatterBrushMask extends AbstractExtentMask {

    private final BlockVector3 position;
    private final int size2;
    private final Mask surface;
    private final LocalBlockVectorSet placed;

    public SplatterBrushMask(
            EditSession editSession,
            BlockVector3 position,
            int size2,
            Mask surface,
            LocalBlockVectorSet placed
    ) {
        super(editSession);
        this.position = position;
        this.size2 = size2;
        this.surface = surface;
        this.placed = placed;
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(vector);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        double dist = vector.distanceSq(position);
        synchronized (placed) {
            if (dist < size2 && !placed.contains(vector) && ThreadLocalRandom.current().nextInt(5) < 2 && surface.test(vector)) {
                placed.add(vector);
                return true;
            }
        }
        return false;
    }

    @Override
    public Mask copy() {
        // There should not be multiple instances to be thread safe
        return this;
    }

}
