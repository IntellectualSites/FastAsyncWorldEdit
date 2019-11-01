package com.boydti.fawe.jnbt.streamer;

public interface ValueReader<T> extends StreamReader<T> {
    void apply(int index, T value);

    default void applyInt(int index, int value) {
        apply(index, (T) (Integer) value);
    }

    default void applyLong(int index, long value) {
        apply(index, (T) (Long) value);
    }

    default void applyFloat(int index, float value) {
        apply(index, (T) (Float) value);
    }

    default void applyDouble(int index, double value) {
        apply(index, (T) (Double) value);
    }
}
