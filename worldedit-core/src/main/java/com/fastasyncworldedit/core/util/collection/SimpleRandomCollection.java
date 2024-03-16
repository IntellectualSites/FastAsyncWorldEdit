package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.random.SimpleRandom;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class SimpleRandomCollection<T> implements RandomCollection<T> {

    private final NavigableMap<Double, T> map;
    private final double total;

    /**
     * Create a {@link RandomCollection} from a weighted map and a RNG.
     * It is recommended to use {@link RandomCollection#of(List)}
     * instead of this constructor.
     *
     * @param weights the weighted map.
     */
    public SimpleRandomCollection(List<Weighted<T>> weights) {
        this.map = new TreeMap<>();
        double total = 0;
        for (Weighted<T> entry : weights) {
            final double weight = entry.weight();
            if (weight <= 0) {
                throw new IllegalArgumentException("Weights must be positive");
            }
            total += weight;
            this.map.put(total, entry.value());
        }
        this.total = total;
    }

    @Override
    public T next(final SimpleRandom random, int x, int y, int z) {
        return map.ceilingEntry(random.nextDouble(x, y, z, this.total)).getValue();
    }

}
