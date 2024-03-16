package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.random.SimpleRandom;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A RandomCollection holds multiple values that can be accessed by using
 * {@link RandomCollection#next(SimpleRandom, int, int, int)}. The returned value is
 * determined by a given {@link SimpleRandom} implementation.
 *
 * @param <T> the type of values the collection holds.
 */
public sealed interface RandomCollection<T> permits FastRandomCollection, SimpleRandomCollection {

    /**
     * Return a new RandomCollection. The implementation may differ depending on the
     * given arguments but there is no need to differ.
     *
     * @param weights the weighted map.
     * @param <T>     the type the collection holds.
     * @return a RandomCollection using the given weights and the RNG.
     */
    static <T> RandomCollection<T> of(List<Weighted<T>> weights) {
        return FastRandomCollection.create(weights)
                .orElseGet(() -> new SimpleRandomCollection<>(weights));
    }

    T next(SimpleRandom random, int x, int y, int z);

    record Weighted<T>(T value, double weight) {

    }
}
