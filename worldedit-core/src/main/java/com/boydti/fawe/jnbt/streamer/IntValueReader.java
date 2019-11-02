package com.boydti.fawe.jnbt.streamer;

import java.io.IOException;

public interface IntValueReader extends ValueReader<Integer> {
    void applyInt(int index, int value) throws IOException;

    @Override
    default void apply(int index, Integer value) throws IOException {
        applyInt(index, value);
    }
}
