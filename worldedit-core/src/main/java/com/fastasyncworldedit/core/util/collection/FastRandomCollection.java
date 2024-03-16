package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.random.SimpleRandom;
import com.fastasyncworldedit.core.util.MathMan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FastRandomCollection<T> implements RandomCollection<T> {

    private final T[] values;

    private FastRandomCollection(T[] values) {
        this.values = values;
    }

    /**
     * Create a new FastRandomCollection if the given values and weights match the criteria. The
     * criteria may change at any point, so this method isn't guaranteed to return a non-empty
     * {@code Optional} in any case.
     *
     * @param weights the weight of the values.
     * @param <T>     the value type.
     * @return an {@link Optional} containing the new collection if it could be created, {@link
     *         Optional#empty()} otherwise.
     * @see RandomCollection for API usage.
     */
    public static <T> Optional<RandomCollection<T>> create(List<Weighted<T>> weights) {
        int max = 0;
        int[] counts = new int[weights.size()];
        double[] weightDoubles = weights.stream().mapToDouble(Weighted::weight).toArray();
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
        for (Weighted<T> entry : weights) {
            int num = (int) (100 * entry.weight());
            for (int j = 0; j < num / gcd; j++) {
                parsed.add(entry.value());
            }
        }
        @SuppressWarnings("unchecked")
        T[] values = (T[]) parsed.toArray();
        FastRandomCollection<T> fastRandomCollection = new FastRandomCollection<>(values);
        return Optional.of(fastRandomCollection);
    }

    @Override
    public T next(final SimpleRandom random, int x, int y, int z) {
        return values[random.nextInt(x, y, z, values.length)];
    }

}
