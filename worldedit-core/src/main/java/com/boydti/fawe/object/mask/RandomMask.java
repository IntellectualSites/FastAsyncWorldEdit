package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.AbstractMask;
import java.util.SplittableRandom;

public class RandomMask extends AbstractMask implements ResettableMask {
    private transient SplittableRandom random;
    private final double threshold;

    public RandomMask(double threshold) {
        this.random = new SplittableRandom();
        this.threshold = (threshold - 0.5) * Integer.MAX_VALUE;
    }

    @Override
    public boolean test(Vector vector) {
        return random.nextInt() <= threshold;
    }

    @Override
    public void reset() {
        random = new SplittableRandom();
    }
}