package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.random.SimpleRandom;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SimpleRandomCollection<E> extends RandomCollection<E> {

    private final NavigableMap<Double, E> map = new TreeMap<>();
    private double total = 0;

    /**
     * Create a {@link RandomCollection} from a weighted map and a RNG.
     * It is recommended to use {@link RandomCollection#of(Map, SimpleRandom)}
     * instead of this constructor.
     *
     * @param weights the weighted map.
     * @param random  the random number generator.
     */
    public SimpleRandomCollection(Map<E, Double> weights, SimpleRandom random) {
        super(random);
        for (Map.Entry<E, Double> entry : weights.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }
    }

    public void add(double weight, E result) {
        if (weight <= 0) {
            return;
        }
        total += weight;
        map.put(total, result);
    }

    @Override
    public E next(int x, int y, int z) {
        return map.ceilingEntry(getRandom().nextDouble(x, y, z, this.total)).getValue();
    }

}
