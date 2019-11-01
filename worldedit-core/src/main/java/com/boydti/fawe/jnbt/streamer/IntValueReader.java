package com.boydti.fawe.jnbt.streamer;

public interface IntValueReader extends ValueReader<Integer> {
    void applyInt(int index, int value);

    @Override
    default void apply(int index, Integer value) {
        applyInt(index, value);
    }
}
