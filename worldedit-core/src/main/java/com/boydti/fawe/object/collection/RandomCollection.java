package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.random.SimpleRandom;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class RandomCollection<T> {
    private SimpleRandom random;

    protected RandomCollection(SimpleRandom random) {
        this.random = random;
    }

    public static <T> RandomCollection<T> of(Map<T, Double> weights, SimpleRandom random) {
        checkNotNull(random);
        return FastRandomCollection.create(weights, random)
                .orElse(new SimpleRandomCollection<>(weights, random));
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
