package com.boydti.fawe.jnbt.streamer;

public interface LongValueReader extends ValueReader<Long> {
    void applyLong(int index, long value);

    @Override
    default void apply(int index, Long value) {
        applyLong(index, value);
    }
}
