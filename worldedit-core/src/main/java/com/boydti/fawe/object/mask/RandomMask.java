package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.SplittableRandom;

public class RandomMask extends AbstractMask implements ResettableMask {
    private transient SplittableRandom random;
    private final double threshold;

    public RandomMask(double threshold) {
        this.random = new SplittableRandom();
        this.threshold = (threshold - 0.5) * Integer.MAX_VALUE;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return random.nextInt() <= threshold;
    }

    @Override
    public void reset() {
        random = new SplittableRandom();
    }

    @Override
    public Mask copy() {
        return new RandomMask(threshold);
    }
}
