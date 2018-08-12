package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.random.SimpleRandom;
import java.util.Map;


import static com.google.common.base.Preconditions.checkNotNull;

public abstract class RandomCollection<T> {
    protected SimpleRandom random;

    public RandomCollection(Map<T, Double> weights, SimpleRandom random) {
        this.random = random;
    }

    public static <T> RandomCollection<T> of(Map<T, Double> weights, SimpleRandom random) {
        try {
            return new FastRandomCollection<>(weights, random);
        } catch (IllegalArgumentException ignore) {
            return new SimpleRandomCollection<>(weights, random);
        }
    }

    public void setRandom(SimpleRandom random) {
        checkNotNull(random);
        this.random = random;
    }

    public SimpleRandom getRandom() {
        return random;
    }

    public abstract T next(int x, int y, int z);
}
