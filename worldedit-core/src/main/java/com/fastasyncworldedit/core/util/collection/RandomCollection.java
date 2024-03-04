package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.random.SimpleRandom;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A RandomCollection holds multiple values that can be accessed by using
 * {@link RandomCollection#next(int, int, int)}. The returned value is
 * determined by a given {@link SimpleRandom} implementation.
 *
 * @param <T> the type of values the collection holds.
 */
public abstract class RandomCollection<T> {

    private SimpleRandom random;

    protected RandomCollection(SimpleRandom random) {
        this.random = random;
    }

    /**
     * Return a new RandomCollection. The implementation may differ depending on the
     * given arguments but there is no need to differ.
     *
     * @param weights the weighted map.
     * @param random  the random number generator.
     * @param <T>     the type the collection holds.
     * @return a RandomCollection using the given weights and the RNG.
     */
    public static <T> RandomCollection<T> of(Map<T, Double> weights, SimpleRandom random) {
        checkNotNull(random);
        return FastRandomCollection.create(weights, random)
                .orElseGet(() -> new SimpleRandomCollection<>(weights, random));
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
