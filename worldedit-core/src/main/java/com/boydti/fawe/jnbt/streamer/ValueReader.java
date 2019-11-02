package com.boydti.fawe.jnbt.streamer;

import java.io.IOException;

public interface ValueReader<T> extends StreamReader<T> {
    void apply(int index, T value) throws IOException;

    default void applyInt(int index, int value) throws IOException {
        apply(index, (T) (Integer) value);
    }

    default void applyLong(int index, long value) throws IOException {
        apply(index, (T) (Long) value);
    }

    default void applyFloat(int index, float value) throws IOException {
        apply(index, (T) (Float) value);
    }

    default void applyDouble(int index, double value) throws IOException {
        apply(index, (T) (Double) value);
    }
}
