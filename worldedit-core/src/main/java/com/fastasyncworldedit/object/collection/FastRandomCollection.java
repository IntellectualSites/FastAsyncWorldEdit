package com.fastasyncworldedit.object.collection;

import com.fastasyncworldedit.object.random.SimpleRandom;
import com.fastasyncworldedit.util.MathMan;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class FastRandomCollection<T> extends RandomCollection<T> {
    private final T[] values;

    private FastRandomCollection(T[] values, SimpleRandom random) {
        super(random);
        this.values = values;
    }

    /**
     * Create a new FastRandomCollection if the given values and weights match the criteria. The
     * criteria may change at any point, so this method isn't guaranteed to return a non-empty
     * {@code Optional} in any case.
     *
     * @param weights the weight of the values.
     * @param random the random generator to use for this collection.
     * @param <T> the value type.
     * @return an {@link Optional} containing the new collection if it could be created, {@link
     *     Optional#empty()} otherwise.
     * @see RandomCollection for API usage.
     */
    public static <T> Optional<RandomCollection<T>> create(Map<T, Double> weights, SimpleRandom random) {
        int max = 0;
        int[] counts = new int[weights.size()];
        Double[] weightDoubles = weights.values().toArray(new Double[0]);
        for (int i = 0; i < weightDoubles.length; i++) {
            int weight = (int) (weightDoubles[i] * 100);
            counts[i] = weight;
            if (weight != (weightDoubles[i] * 100)) {
                return Optional.empty();
            }
            if (weight > max) {
                max = weight;
            }
        }
        int gcd = MathMan.gcd(counts);
        if (max / gcd > 100000) {
            return Optional.empty();
        }
        ArrayList<T> parsed = new ArrayList<>();
        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            int num = (int) (100 * entry.getValue());
            for (int j = 0; j < num / gcd; j++) {
                parsed.add(entry.getKey());
            }
        }
        @SuppressWarnings("unchecked")
        T[] values = (T[]) parsed.toArray();
        FastRandomCollection<T> fastRandomCollection = new FastRandomCollection<>(values, random);
        return Optional.of(fastRandomCollection);
    }

    @Override
    public T next(int x, int y, int z) {
        return values[getRandom().nextInt(x, y, z, values.length)];
    }
}
