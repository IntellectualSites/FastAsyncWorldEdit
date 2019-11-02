package com.boydti.fawe.jnbt.streamer;

import java.io.IOException;

public interface LongValueReader extends ValueReader<Long> {
    void applyLong(int index, long value) throws IOException;

    @Override
    default void apply(int index, Long value) throws IOException {
        applyLong(index, value);
    }
}
