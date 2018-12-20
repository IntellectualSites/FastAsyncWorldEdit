package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.random.SimpleRandom;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SimpleRandomCollection<E> extends RandomCollection<E> {

    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private double total = 0;

    public SimpleRandomCollection(Map<E, Double> weights, SimpleRandom random) {
        super(weights, random);
        for (Map.Entry<E, Double> entry : weights.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }
    }

    public void add(double weight, E result) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, result);
    }

    public E next(int x, int y, int z) {
        return map.ceilingEntry(random.nextDouble(x, y, z)).getValue();
    }
}
